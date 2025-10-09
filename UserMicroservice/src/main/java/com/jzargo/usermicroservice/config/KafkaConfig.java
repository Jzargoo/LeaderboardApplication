package com.jzargo.usermicroservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String PULSE_LEADERBOARD
            = "pulse-leaderboard-topic";
    @Bean
    NewTopic pulseLeaderboard(){
        return TopicBuilder
                .name(PULSE_LEADERBOARD)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }
}
