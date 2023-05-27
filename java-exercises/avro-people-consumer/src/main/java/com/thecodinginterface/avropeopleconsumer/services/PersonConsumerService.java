package com.thecodinginterface.avropeopleconsumer.services;

import com.thecodinginterface.avrodomainevents.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PersonConsumerService {

    //loger
    private static final Logger logger = LoggerFactory.getLogger(PersonConsumerService.class);


    //kafka listener
    @KafkaListener(topics = "${topics.people-avro.name}", containerFactory = "kafkaListenerContainerFactory")
    public void listenForEvents(Person person) {
        logger.info("Received Message in group foo: " + person);
    }

}
