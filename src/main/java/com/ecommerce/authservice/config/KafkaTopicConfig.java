package com.ecommerce.authservice.config;

// ============================================================
// KAFKA TOPIC CONFIG — TEMPORARILY DISABLED (REST mode active)
// Uncomment this entire class to re-enable Kafka topic creation.
// ============================================================

/*
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic userRegisteredTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.getUserRegistered())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userLoginTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.getUserLogin())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userCreatedTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.getUserCreated())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userRegisteredDltTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.getUserRegistered() + ".dlt")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
*/

// Empty placeholder — required to keep the package valid.
public class KafkaTopicConfig {
}
