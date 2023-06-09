package com.thecodinginterface.orderprocessing.configs;

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
@EnableKafkaStreams
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    String schemaRegUrl;

    @Value("${topics.order-validated.name}")
    String orderValidationTopic;

    @Value("${topics.common.partitions}")
    int defaultPartitions;

    @Value("${topics.common.replicas}")
    int defaultReplicas;

    @Value("${topics.common.retention-ms}")
    int defaultRetention;

    static final String ORDER_PROCESSING_APP_ID = "order-processing-app";

    @Bean
    public NewTopic orderValidationTopic() {
        return TopicBuilder.name(orderValidationTopic)
                .replicas(defaultReplicas)
                .partitions(defaultPartitions)
                .config(TopicConfig.RETENTION_MS_CONFIG, Integer.toString(defaultRetention))
                .build();
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
        return new KafkaStreamsConfiguration(Map.of(
                StreamsConfig.APPLICATION_ID_CONFIG, ORDER_PROCESSING_APP_ID,
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName(),
                StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class,
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegUrl
        ));
    }
}
