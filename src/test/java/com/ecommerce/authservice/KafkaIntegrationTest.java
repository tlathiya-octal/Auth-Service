package com.ecommerce.authservice;

import com.ecommerce.authservice.config.KafkaTopicsProperties;
import com.ecommerce.authservice.dto.RegisterRequest;
import com.ecommerce.authservice.dto.RegisterResponse;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.events.UserRegisteredEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"user.registered"})
class KafkaIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

    @Test
    void registerPublishesUserRegisteredEvent() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test-user@example.com")
                .password("Password123!")
                .build();

        RegisterResponse response = authService.register(request);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "auth-service-test-consumer",
                "false",
                embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        JsonDeserializer<UserRegisteredEvent> deserializer = new JsonDeserializer<>(UserRegisteredEvent.class, objectMapper);
        deserializer.addTrustedPackages("com.ecommerce.events");

        Consumer<String, UserRegisteredEvent> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                deserializer
        ).createConsumer();

        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, kafkaTopicsProperties.getUserRegistered());
        ConsumerRecord<String, UserRegisteredEvent> record = KafkaTestUtils.getSingleRecord(
                consumer,
                kafkaTopicsProperties.getUserRegistered()
        );

        assertThat(record.value()).isNotNull();
        assertThat(record.value().userId()).isEqualTo(response.getUserId());
        assertThat(record.value().email()).isEqualTo(request.getEmail().toLowerCase());

        consumer.close();
    }
}
