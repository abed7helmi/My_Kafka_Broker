package org.sid.demospringcloudstreamskafka.web;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.*;
import org.sid.demospringcloudstreamskafka.entities.PageEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


@RestController
public class PageEventRestController {

    @Autowired
    private StreamBridge streamBridge; // fonctionne avec n'importe quel broker
    @Autowired
    private InteractiveQueryService interactiveQueryService;

    //publie msg dans un topic
    @GetMapping("/publish/{topic}/{name}")
    public PageEvent publish(@PathVariable String topic, @PathVariable String name){
        PageEvent pageEvent=new PageEvent(name,Math.random()>0.5?"U1":"U2",new Date(),new Random().nextInt(9000));
        streamBridge.send(topic,pageEvent);
        return pageEvent;
    }
    @GetMapping(path = "/analytics",produces = MediaType.TEXT_EVENT_STREAM_VALUE) //server sent event : envoyer les données dés que le client se connecte , utlise spring web stream et retourner un type flux
    public Flux<Map<String, Long>>  analytics(){
        return Flux.interval(Duration.ofSeconds(1)) // pour chaque seconde , un enregersitrement dans ce stream
                .map(sequence->{
                    Map<String,Long> stringLongMap=new HashMap<>(); // clé nom de la page , value nb de visite
                    ReadOnlyWindowStore<String, Long> windowStore = interactiveQueryService.getQueryableStore("page-count", QueryableStoreTypes.windowStore()); // interroger le store
                    Instant now=Instant.now(); // now
                    Instant from=now.minusMillis(5000); // now - 5s
                    KeyValueIterator<Windowed<String>, Long> fetchAll = windowStore.fetchAll(from, now); // fetch c comme select
                    //WindowStoreIterator<Long> fetchAll = windowStore.fetch(page, from, now);
                    while (fetchAll.hasNext()){
                        KeyValue<Windowed<String>, Long> next = fetchAll.next();
                        stringLongMap.put(next.key.key(),next.value);
                    }
                    return stringLongMap;
                }).share(); // partager par plusieurs users , si plusiseurs se connecte ils recoivent le méme flux
    }

}

