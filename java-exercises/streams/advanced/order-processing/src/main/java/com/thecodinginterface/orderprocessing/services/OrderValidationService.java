package com.thecodinginterface.orderprocessing.services;

import com.thecodinginterface.ordersdomainevents.Order;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
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

    static final Logger logger = LoggerFactory.getLogger(OrderValidationService.class);

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    String schemaRegUrl;

    @Value("${topics.order-created.name}")
    String orderCreationTopic;

    @Value("${topics.order-validated.name}")
    String orderValidationTopic;


    static final double VALID_PROPORTION = 0.95;

    public Map<String, ?> serdeConfig() {
        return Map.of(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegUrl);
    }



    @Autowired
    public void process(StreamsBuilder streamsBuilder) {
        var orderSerde = new SpecificAvroSerde<Order>();
        orderSerde.configure(serdeConfig(), false);

        KStream<Integer, Order> ordersStream = streamsBuilder.stream(orderCreationTopic,
                Consumed.with(Serdes.Integer(), orderSerde));

        ordersStream.mapValues(order -> {
            var randNum = ThreadLocalRandom.current().nextDouble(); // 0 -> 1
            return Order.newBuilder(order)
                    .setValid(randNum <= VALID_PROPORTION)
                    .build();
        })
        .peek((k, v) -> logger.info("output order {}", v))
        .to(orderValidationTopic);
    }
}
