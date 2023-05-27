package com.thecodinginterface.avropeopleproducer.controllers;

import com.github.javafaker.Faker;
import com.thecodinginterface.avrodomainevents.Person;
import com.thecodinginterface.avropeopleproducer.commands.CreatePeopleCommand;
import com.thecodinginterface.avropeopleproducer.models.PersonDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class PeopleController {

    static final Logger logger = LoggerFactory.getLogger(PeopleController.class);

    @Value("${topics.people-avro.name}")
    private String peopleTopic;

    private KafkaTemplate<String, Person> kafkaTemplate;

    public PeopleController(KafkaTemplate<String, Person> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/people")
    public String helloPeople() {
        return "Hello People";
    }

    @PostMapping("/people")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PersonDTO> create(@RequestBody CreatePeopleCommand cmd) {
        logger.info("create command is " + cmd);

        var faker = new Faker();
        List<PersonDTO> people = new ArrayList<>();
        for (int i = 0; i < cmd.getCount(); i++) {
            // Avro object to send
            var person = new Person();
            person.setFirstName(faker.name().firstName());
            person.setLastName(faker.name().lastName());
            person.setTitle(faker.job().title());

            // DTO to return from REST endpoint
            people.add(new PersonDTO(person.getFirstName(), person.getLastName(), person.getTitle()));

            CompletableFuture<SendResult<String, Person>> future = kafkaTemplate
                    .send(
                            peopleTopic,
                            (person.getFirstName() + person.getLastName()).toLowerCase().replaceAll("\\s+", "-"),
                            person
                    );

            try {
                var result = future.get();
                logger.info(
                        "\n  Published person=" + person
                                + ",\n  partition=" + result.getRecordMetadata().partition()
                                + ",\n  offset=" + result.getRecordMetadata().offset()
                );
            } catch (Exception e) {
                logger.error("Failed to publish " + person, e);
            }
        }
        kafkaTemplate.flush();
        return people;
    }

}

