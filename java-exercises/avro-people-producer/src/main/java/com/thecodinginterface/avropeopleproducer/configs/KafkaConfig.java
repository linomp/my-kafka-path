package com.thecodinginterface.avropeopleproducer.configs;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {


    @Value("${topics.people-avro.name}")
    String topicName;

    @Value("${topics.people-avro.replicas}")
    int topicReplicas;

    @Value("${topics.people-avro.partitions}")
    int topicPartitions;

    @Bean
    public NewTopic makePeopleAvroTopic() {
        return TopicBuilder.name(topicName)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }
}
