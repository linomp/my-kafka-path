package com.thecodinginterface.orderprocessing.services;

import com.thecodinginterface.ordersdomainevents.*;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomerRevenueService {

    static final Logger logger = LoggerFactory.getLogger(CustomerRevenueService.class);

    @Value("${spring.kafka.bootstrap-servers}")
    String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    String schemaRegUrl;

    @Value("${topics.order-validated.name}")
    String orderValidationTopic;

    @Value("${topics.customers.name}")
    String customersTopic;

    @Value("${topics.products.name}")
    String productsTopic;

    @Value("${topics.customer-revenue.name}")
    String customerRevenueTopic;

    public Map<String, ?> serdeConfig() {
        return Map.of(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegUrl);
    }

    @Autowired
    public void process(StreamsBuilder streamsBuilder) {

        // Filter out invalid orders
        var orderSerde = new SpecificAvroSerde<Order>();
        orderSerde.configure(serdeConfig(), false);
        KStream<Integer, Order> ordersStream = streamsBuilder.stream(orderValidationTopic, Consumed.with(Serdes.Integer(), orderSerde))
                .filter((k, v) -> v.getValid()).peek((key, order) -> logger.info("Processing valid order: " + order));

        // Enrich order events with Product information
        var productSerde = new SpecificAvroSerde<Product>();
        productSerde.configure(serdeConfig(), false);
        // 1. create a GlobalKTable for products
        GlobalKTable<Integer, Product> products = streamsBuilder.globalTable(productsTopic, Consumed.with(Serdes.Integer(), productSerde));
        // 2. join orders with products
        KStream<Integer, ProductOrder> customerProductOrders = ordersStream.join(
                products,
                (orderId, order) -> order.getProductId(),
                (order, product) -> ProductOrder.newBuilder()
                        .setId(order.getId())
                        .setCustomerId(order.getCustomerId())
                        .setProductId(order.getProductId())
                        .setProductPrice(product.getPrice())
                        .setProductQuantity(order.getProductQty())
                        .setCreatedMs(order.getCreatedMs())
                        .setProductName(product.getName())
                        .build()
        );

        // Calc order revenue & map to customer id
        KStream<Integer, Integer> customerOrderRevStream = customerProductOrders.map((k, v) -> {
            var orderRevenue = v.getProductQuantity() * v.getProductPrice();
            return new KeyValue<>(v.getCustomerId(), orderRevenue);
        });

        // Group by customer id
        KGroupedStream<Integer, Integer> customerOrderGroups = customerOrderRevStream.groupByKey(
                Grouped.with(Serdes.Integer(), Serdes.Integer()));

        // Sum order revenue by customer id
        KTable<Integer, Integer> customerRevenueTable = customerOrderGroups.reduce(Integer::sum);

        // Enrich customer revenue events with customer info
        var customerSerde = new SpecificAvroSerde<Customer>();
        customerSerde.configure(serdeConfig(), false);
        // 1. create a GlobalKTable for customers
        GlobalKTable<Integer, Customer> customers = streamsBuilder.globalTable(customersTopic, Consumed.with(Serdes.Integer(), customerSerde));
        // 2. join customer revenue with customers
        KStream<Integer, CustomerRevenue> customerRevenueStream = customerRevenueTable.toStream()
                .join(
                        customers,
                        (k, v) -> k,
                        (revenue, customer) -> CustomerRevenue.newBuilder()
                                .setId(customer.getId())
                                .setRevenue(revenue)
                                .setName(customer.getName())
                                .build()
                );

        // Write customer revenue events to topic
        var customerRevenueSerde = new SpecificAvroSerde<CustomerRevenue>();
        customerRevenueSerde.configure(serdeConfig(), false);
        customerRevenueStream.to(customerRevenueTopic, Produced.with(Serdes.Integer(), customerRevenueSerde));
        
    }

}
