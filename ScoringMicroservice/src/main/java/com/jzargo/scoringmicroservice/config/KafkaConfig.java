package com.jzargo.scoringmicroservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String COMMAND_STRING_SCORE_TOPIC = "user-string-score-command-topic";
    public static final String USER_EVENT_SCORE_TOPIC = "user-event-score-topic";
    public static final String LEADERBOARD_EVENT_TOPIC = "leaderboard-event-topic";
    public static final String MESSAGE_ID = "message-id";

    @Bean
    public NewTopic userEventScoreTopic(){
        return TopicBuilder
                .name(USER_EVENT_SCORE_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic leaderboardEventTopic(){
        return TopicBuilder
                .name(LEADERBOARD_EVENT_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic commandStringScoreTopic(){
        return TopicBuilder
                .name(COMMAND_STRING_SCORE_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }
}
