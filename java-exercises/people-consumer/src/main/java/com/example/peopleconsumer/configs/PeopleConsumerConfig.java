package com.example.peopleconsumer.configs;

import com.example.peopleconsumer.entities.Person;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@EnableKafka
@Configuration
public class PeopleConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${topics.people-basic.name}")
    private String topicName;

    public Map<String,Object> consumerConfigs(){
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG,"people.advanced.java.grp-0"
        );
    }

    // Our consumer will get wrapped by a concurrent kafka listener.

    // First we create a consumer factory bean, which returns an instance of the default KafkaConsumerFactory.
    @Bean
    public ConsumerFactory<String, Person> consumerFactory(){
        return new DefaultKafkaConsumerFactory<>(
                consumerConfigs(),
                new StringDeserializer(),
                // the producer can indicate fully qualified types on the headers, to help de-serialize.
                // But here we ignore that because the Person classes we have may conflict
                new JsonDeserializer<>(Person.class, false)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String,Person> personListenerContainerFactory(){

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Person>();
        factory.setConsumerFactory(consumerFactory());

        // By default it does At-least-once processing (AckMode.BATCH)
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

}
