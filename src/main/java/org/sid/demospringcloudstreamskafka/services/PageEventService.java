package org.sid.demospringcloudstreamskafka.services;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.sid.demospringcloudstreamskafka.entities.PageEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.Date;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class PageEventService {

    @Bean
    public Consumer<PageEvent> pageEventConsumer(){ // spring fait le subscribe automatiqyement
        return (input)->{
            System.out.println("**********************");
            System.out.println(input.toString());
            System.out.println("**********************");
        };
    }
    @Bean
    public Supplier<PageEvent> pageEventSupplier(){ // envoie des msg chaque seconde , changer le timer dans properties si on veux
        return ()-> new PageEvent(
                Math.random()>0.5?"P1":"P2",
                Math.random()>0.5?"U1":"U2",
                new Date(),
                new Random().nextInt(9000)); //
    }
    @Bean
    public Function<PageEvent/* entrée */,PageEvent /*sortie */> pageEventFunction(){  // consumer & producer
        return (input)->{
            input.setName("Page:"+input.getName().length());
            input.setUser("User=>1");
            return input;
        };
    }
    @Bean
    public Function<KStream<String,PageEvent> /* entrée */, KStream<String,Long> /* sortie des statics , page+ nb de visite*/> kStreamFunction(){
        return (input)->{
            return input
                    .filter((k,v)->v.getDuration()>100)// traiter que les evenement que la durée de visite depasse 100
                    .map((k,v)->new KeyValue<>(v.getName(),0L)) // produire un stream , clé le nom et la valeur est 0
                    .groupBy((k,v)->k,Grouped.with(Serdes.String(),Serdes.Long()))// grouper p/p la clé ,  produit type ktable c pour ca on fait ro stream
                    .windowedBy(TimeWindows.of(Duration.ofSeconds(5))) // les stats des 5 derniére secondes , produit un clé de type window
                    .count(Materialized.as("page-count"))// un count , Materialized => vue matralize => persister le resultat dans un stor pour l'afficher dans l'app
                    .toStream()
                    .map((k,v)->new KeyValue<>("=>"+k.window().startTime()+k.window().endTime()+":"+k.key(),v));
        };
    }
}
