package com.thecodinginterface.avropeopleconsumer.configs;

import com.thecodinginterface.avrodomainevents.Person;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.Map;

@EnableKafka
@Configuration
public class PeopleConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    @Value("${spring.kafka.consumer.properties.schema.registry.url}")
    String schemaRegistryUrl;

    //groupid
    @Value("${spring.kafka.consumer.properties.group.id}")
    private String groupId;


    //configs map with avro schema
    public Map<String, Object> consumerConfigs() {
        return Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroDeserializer.class,
                ConsumerConfig.GROUP_ID_CONFIG, groupId,
                "schema.registry.url", schemaRegistryUrl,
                "specific.avro.reader", "true"
        );
    }

    // Consumer Factory bean
    @Bean
    public ConsumerFactory<String, Person> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }


    // ConcurrentKafkaLister container factory bean
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Person> kafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, Person>();
        factory.setConsumerFactory(consumerFactory());

        // commit offset as we consume each record
        // TODO: difference with MANUAL_IMMEDIATE?
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);

        return factory;
    }

}

