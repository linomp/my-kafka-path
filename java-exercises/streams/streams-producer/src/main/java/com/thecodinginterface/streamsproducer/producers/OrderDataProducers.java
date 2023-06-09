package com.thecodinginterface.streamsproducer.producers;

import com.thecodinginterface.ordersdomainevents.Customer;
import com.thecodinginterface.ordersdomainevents.Order;
import com.thecodinginterface.ordersdomainevents.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Random;

@Component
public class OrderDataProducers implements ApplicationListener<ApplicationStartedEvent> {

    static final Logger logger = LoggerFactory.getLogger(OrderDataProducers.class);

    @Value("${topics.customers.name}")
    String customersTopic;

    @Value("${topics.products.name}")
    String productsTopic;

    @Value("${topics.order-created.name}")
    String ordersTopic;

    final KafkaTemplate<Integer, Customer> customerKafkaTmpl;
    final KafkaTemplate<Integer, Product> productKafkaTmpl;
    final KafkaTemplate<Integer, Order> orderKafkaTmpl;

    public OrderDataProducers(
            KafkaTemplate<Integer, Customer> customerKafkaTmpl,
            KafkaTemplate<Integer, Product> productKafkaTmpl,
            KafkaTemplate<Integer, Order> orderKafkaTmpl
    ) {
        this.customerKafkaTmpl = customerKafkaTmpl;
        this.productKafkaTmpl = productKafkaTmpl;
        this.orderKafkaTmpl = orderKafkaTmpl;
    }

    public List<Customer> makeCustomers() {
        return List.of(
                new Customer(1000, "Acme Corp."),
                new Customer(1001, "Hooli Corp"),
                new Customer(1002, "Wayne Enterprise"),
                new Customer(1003, "Mystery Inc")
        );
    }

    public List<Product> makeProducts() {
        return List.of(
                new Product(5000, "Wonka Bars", 5),
                new Product(5100, "Chia Pet", 18),
                new Product(5200, "Stress Ball", 4),
                new Product(5300, "Back Scratcher", 7)
        );
    }

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        List<Customer> customers = makeCustomers();
        List<Product> products = makeProducts();

        for (var customer : customers) {
            customerKafkaTmpl.send(customersTopic, customer.getId(), customer)
                    .addCallback(result -> logger.info("Published customer {}", customer),
                            ex -> logger.error("Failed to published customer " + customer, ex));
        }
        customerKafkaTmpl.flush();

        for (var product : products) {
            productKafkaTmpl.send(productsTopic, product.getId(), product)
                    .addCallback(result -> logger.info("Published product {}", product),
                            ex -> logger.error("Failed to publish product " + product, ex));
        }
        productKafkaTmpl.flush();

        var orderId = (int) (System.currentTimeMillis() / 1_000_000L);
        var random = new Random();
        while(true) {
            var customer = customers.get(random.nextInt(customers.size()));
            var product = products.get(random.nextInt(products.size()));

            var qty = random.nextInt(1000) + 1;
            var order = Order.newBuilder()
                    .setId(orderId++)
                    .setCustomerId(customer.getId())
                    .setProductId(product.getId())
                    .setProductQty(qty)
                    .setCreatedMs(Instant.now().toEpochMilli())
                    .setValid(false)
                    .build();
            orderKafkaTmpl.send(ordersTopic, order.getId(), order)
                    .addCallback(result -> logger.info("Produced order {}", order),
                            ex -> logger.error("Failed to publish order " + order, ex));
            if (orderId % 3 == 0) {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    logger.error("Failed to sleep ", e);
                }
            }
        }
    }
}
