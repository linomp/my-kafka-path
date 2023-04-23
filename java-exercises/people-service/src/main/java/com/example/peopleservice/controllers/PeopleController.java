package com.example.peopleservice.controllers;

import com.example.peopleservice.commands.CreatePeopleCommand;
import com.example.peopleservice.entities.Person;
import com.github.javafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PeopleController {
    private static final Logger logger = LoggerFactory.getLogger(PeopleController.class);

    // get topic name from application.yaml file as @Value
    @Value("${topics.people-basic.name}")
    private String peopleTopic;

    // kafka template is a wrapper for the Producer API
    private KafkaTemplate<String, Person> kafkaTemplate;

    public PeopleController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") KafkaTemplate<String, Person> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/people")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Person> create(@RequestBody CreatePeopleCommand command) {
        logger.info("Command is: " + command);


        var faker = new Faker();
        List<Person> people = new ArrayList<>();

        for (int i = 0; i < command.getCount(); i++) {
            var person = new Person(
                    UUID.randomUUID().toString(),
                    faker.name().fullName(),
                    faker.job().title()
            );
            people.add(person);
            ListenableFuture<SendResult<String, Person>> future = kafkaTemplate.send(
                    peopleTopic, person.getTitle().toLowerCase().replaceAll("\\s+", "-"),
                    person);
            future.addCallback(
                    result -> {
                        assert result != null;
                        logger.info("Sent message=[{}] with offset=[{}]", person, result.getRecordMetadata().offset());
                    },
                    ex -> logger.error("Unable to send message=[{}] due to : {}", person, ex.getMessage())
            );
        }

        kafkaTemplate.flush();

        return people;
    }

}
