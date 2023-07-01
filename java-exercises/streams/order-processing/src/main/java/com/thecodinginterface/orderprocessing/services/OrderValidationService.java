package com.thecodinginterface.orderprocessing.services;

import com.thecodinginterface.ordersdomainevents.Order;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OrderValidationService.class);

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    String schemaRegUrl;

    @Value("${topics.common.replicas}")
    int defaultReplicas;

    @Value("${topics.common.retention-ms}")
    int defaultRetention;

    @Value("${topics.products.cleanup-policy}")
    String productsTopicCleanupPolicy;

    @Value("${topics.order-created.name}")
    String orderCreatedTopic;


    @Value("${topics.order-validated.name}")
    String orderValidatedTopic;

    // constant for random invalid assignment based on probability
    static final double VALID_ORDER_PROBABILITY = 0.5;


    public Map<String, Object> valueSerdeConfig() {
        return Map.of(
                AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegUrl
        );
    }

    @Autowired
    public void process(StreamsBuilder builder) {
        // make streams builder
        // try with
        try (var orderSerde = new SpecificAvroSerde<Order>()) {
            orderSerde.configure(valueSerdeConfig(), false);

            KStream<Integer, Order> orderCreatedStream = builder.stream(
                    orderCreatedTopic,
                    Consumed.with(Serdes.Integer(), orderSerde)
            );

            orderCreatedStream.mapValues(order -> {
                        // ThreadLocalRandom is preferred for multithreaded environments
                        // because there is no sharing of a global instance of Random
                        var randNum = ThreadLocalRandom.current().nextDouble(0, 1);

                        // apply a random probability to the order validity
                        return Order.newBuilder(order)
                                .setValid(randNum <= VALID_ORDER_PROBABILITY)
                                .build();
                    })
//                    .peek((key, order) -> logger.info("Order validated: " + order))
                    .to(orderValidatedTopic);
        }
    }


}
