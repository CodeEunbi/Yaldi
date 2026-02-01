package com.yaldi.infra.kafka.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    // Example topic - 필요한 토픽들을 여기에 추가하세요
    @Bean
    public NewTopic exampleTopic() {
        return TopicBuilder.name("yaldi.example.topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic collaborationTopic() {
        return TopicBuilder.name("yaldi.collaboration.topic")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic mockDataCreateTopic() {
        return TopicBuilder.name("yaldi.mockdata.create")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic versionVerificationTopic() {
        return TopicBuilder.name("yaldi.version.verification")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
