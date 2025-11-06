package com.jzargo.usermicroservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String PULSE_LEADERBOARD
            = "pulse-leaderboard-topic";
    public static final String SAGA_CREATE_LEADERBOARD_TOPIC = "saga-create-leaderboard-topic";
    public static final String GROUP_ID = "users-group" ;
    public static final String MESSAGE_ID_HEADER = "message-id";
    public static final String SAGA_ID_HEADER = "saga-id";
    @Value("${kafka.partitionCount}")
    private Integer partitionCount;
    @Value("${kafka.replicas.count}")
    private Integer replicasCount;
    @Value("${kafka.partition.minInSync}")
    private Integer minInSyncReplicas;

    @Bean
    NewTopic pulseLeaderboard(){
        return TopicBuilder
                .name(PULSE_LEADERBOARD)
                .partitions(partitionCount)
                .replicas(replicasCount)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }
    @Bean
    NewTopic sagaCreateLeaderboardTopic(){
        return TopicBuilder
                .name(SAGA_CREATE_LEADERBOARD_TOPIC)
                .partitions(partitionCount)
                .replicas(replicasCount)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }
}
