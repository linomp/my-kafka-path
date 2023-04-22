package com.example.peopleservice.configs;


import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Configuration
public class KafkaConfig {

    static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);
    public static final String DEFAULT_RETENTION_MS = "720000";

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${topics.people-basic.name}")
    private String topicName;

    @Value("${topics.people-basic.partitions}")
    private int topicPartitions;

    @Value("${topics.people-basic.replicas}")
    private int topicReplicas;

    @Bean
    public NewTopic peopleBasicTopic() {
        return TopicBuilder.name(topicName)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    @Bean
    public NewTopic peopleBasicShortTopic() {
        return TopicBuilder.name(topicName + "-short")
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .config("retention.ms", DEFAULT_RETENTION_MS)
                .build();
    }

    // in spring boot lifecycle, this method is called after all beans are created
    @PostConstruct
    public void changePeopleBasicTopicRetention() {
        // create a connection with configs to bootstrap servers
        Map<String, Object> configs = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (var admin = AdminClient.create(configs)) {
            // create a config resource to fetch the config of the topic
            var configResource = new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, topicName);

            // fetch the retention.ms config of the topic
            var config = admin.describeConfigs(Collections.singleton(configResource)).values().get(configResource).get();

            if (!config.get(TopicConfig.RETENTION_MS_CONFIG).value().equals(DEFAULT_RETENTION_MS)) {
                var alterConfigEntry = new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, DEFAULT_RETENTION_MS);
                var alterOp = new AlterConfigOp(alterConfigEntry, AlterConfigOp.OpType.SET);
                Map<ConfigResource, Collection<AlterConfigOp>> alterConfigs = Map.of(configResource, Collections.singleton(alterOp));

                // execute the operation with incrementAlterConfigs
                admin.incrementalAlterConfigs(alterConfigs).all().get();
                logger.info("Changed retention.ms of topic {} to {}", topicName, DEFAULT_RETENTION_MS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
