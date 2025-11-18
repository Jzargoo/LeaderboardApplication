package com.jzargo.leaderboardmicroservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig{
    //Consume push events
    public static final String LEADERBOARD_EVENT_TOPIC = "leaderboard-event-topic";
    public static final String LEADERBOARD_UPDATE_TOPIC = "leaderboard-update-topic";
    public static final String MESSAGE_ID = "message-id";
    //Updating cached user
    public static final String USER_STATE_EVENT_TOPIC = "user-state-event-topic";
    // Consume and produce events of create leaderboard
    public static final String SAGA_CREATE_LEADERBOARD_TOPIC = "saga-create-leaderboard-topic";
    public static final String SAGA_ID_HEADER = "saga-id";
    public static final String GROUP_ID = "leaderboard-group";

    @Value("${kafka.topic.insync-replicas}")
    private int minInSyncReplicas;

    @Value("${kafka.topic.replicas}")
    private int replicas;

    @Bean
    public NewTopic leaderboardEventsTopic(){
        return TopicBuilder
                .name(LEADERBOARD_EVENT_TOPIC)
                .partitions(3)
                .replicas(replicas)
                .config("min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }

    @Bean
    public NewTopic userStateEventTopic(){
        return TopicBuilder
                .name(USER_STATE_EVENT_TOPIC)
                .partitions(3)
                .replicas(replicas)
                .config("min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }


    @Bean
    public NewTopic leaderboardUpdateTopic(){
        return TopicBuilder
                .name(LEADERBOARD_UPDATE_TOPIC)
                .partitions(3)
                .replicas(replicas)
                .config("min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }

}
