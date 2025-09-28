package com.jzargo.leaderboardmicroservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig{
    public static final String LEADERBOARD_EVENT_TOPIC = "leaderboard-event-topic";
    public static final String LEADERBOARD_UPDATE_TOPIC = "leaderboard-update-topic";
    public static final String MESSAGE_ID = "message-id";

    @Bean
    public NewTopic leaderboardEventsTopic(){
        return TopicBuilder
                .name(LEADERBOARD_EVENT_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic leaderboardUpdateTopic(){
        return TopicBuilder
                .name(LEADERBOARD_UPDATE_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

}
