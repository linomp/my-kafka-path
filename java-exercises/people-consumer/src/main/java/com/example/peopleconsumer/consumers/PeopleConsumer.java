package com.example.peopleconsumer.consumers;

import com.example.peopleconsumer.entities.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PeopleConsumer {
    static final Logger logger = LoggerFactory.getLogger(PeopleConsumer.class);

     @KafkaListener(topics = "people.basic.java", containerFactory = "personListenerContainerFactory")
     public void handle(Person person){
         logger.info("Consumed person: {}", person);
     }

}
