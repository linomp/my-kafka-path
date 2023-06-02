package com.thecodinginterface.streamsproducer.configs;

import com.thecodinginterface.ordersdomainevents.Customer;
import com.thecodinginterface.ordersdomainevents.Order;
import com.thecodinginterface.ordersdomainevents.Product;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    String schemaRegUrl;

    @Value("${topics.common.partitions}")
    int defaultPartitions;

    @Value("${topics.common.replicas}")
    int defaultReplicas;

    @Value("${topics.common.retention-ms}")
    int defaultRetention;

    @Value("${topics.customers.name}")
    String customersTopic;

    @Value("${topics.customers.cleanup-policy}")
    String customersTopicCleanupPolicy;

    @Value("${topics.products.name}")
    String productsTopic;

    @Value("${topics.products.cleanup-policy}")
    String productsTopicCleanupPolicy;

    @Value("${topics.order-created.name}")
    String orderCreatedTopic;

    @Value("${topics.order-created.cleanup-policy}")
    String getOrderCreatedTopicCleanupPolicy;

    @Bean
    public KafkaAdmin.NewTopics orderDataTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(customersTopic)
                        .partitions(defaultPartitions)
                        .replicas(defaultReplicas)
                        .configs(Map.of(
                                TopicConfig.RETENTION_MS_CONFIG, Integer.toString(defaultRetention),
                                TopicConfig.CLEANUP_POLICY_CONFIG, customersTopicCleanupPolicy
                        ))
                        .build(),
                TopicBuilder.name(productsTopic)
                        .partitions(defaultPartitions)
                        .replicas(defaultReplicas)
                        .configs(Map.of(
                                TopicConfig.RETENTION_MS_CONFIG, Integer.toString(defaultRetention),
                                TopicConfig.CLEANUP_POLICY_CONFIG, productsTopicCleanupPolicy
                        ))
                        .build(),
                TopicBuilder.name(orderCreatedTopic)
                        .partitions(defaultPartitions)
                        .replicas(defaultReplicas)
                        .configs(Map.of(
                                TopicConfig.RETENTION_MS_CONFIG, Integer.toString(defaultRetention),
                                TopicConfig.CLEANUP_POLICY_CONFIG, getOrderCreatedTopicCleanupPolicy
                        ))
                        .build()
        );
    }

    public Map<String, Object> producerConfigs() {
        return Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.IntegerSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, io.confluent.kafka.serializers.KafkaAvroSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.LINGER_MS_CONFIG, 100,
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true,
                ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1,
                ProducerConfig.RETRIES_CONFIG, 5,
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegUrl
        );
    }

    @Bean
    public ProducerFactory<Integer, Customer> customerProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public ProducerFactory<Integer, Product> productProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public ProducerFactory<Integer, Order> orderProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    public KafkaTemplate<Integer, Customer> customerKafkaTemplate() {
        return new KafkaTemplate<>(customerProducerFactory());
    }

    @Bean
    public KafkaTemplate<Integer, Product> productKafkaTemplate() {
        return new KafkaTemplate<>(productProducerFactory());
    }

    @Bean
    public KafkaTemplate<Integer, Order> orderKafkaTemplate() {
        return new KafkaTemplate<>(orderProducerFactory());
    }
}
