package com.jzargo.websocketapi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    //for scoring service
    public static final String USER_EVENT_SCORE_TOPIC = "user-event-score-topic";
    //Directly for leaderboard service
    public static final String LEADERBOARD_UPDATE_TOPIC = "leaderboard-update-topic";

    public static final String GROUP_ID = "websocket-api-group";
    public static final String MESSAGE_ID = "message-id";
    @Value("${kafka.topic.insync-replicas:1}")
    private short minInSyncReplicas;

    @Value("${kafka.topic.replicas:2}")
    private short replicas;
    @Bean
    public NewTopic userEventScoreTopic(){
        return TopicBuilder
                .name(USER_EVENT_SCORE_TOPIC)
                .partitions(3)
                .replicas(replicas)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }

    @Bean
    public NewTopic leaderboardUpdateTopic(){
        return TopicBuilder
                .name(LEADERBOARD_UPDATE_TOPIC)
                .partitions(3)
                .replicas(replicas)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }
}
