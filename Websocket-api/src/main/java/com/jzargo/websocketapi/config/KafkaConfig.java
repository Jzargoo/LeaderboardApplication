package com.jzargo.websocketapi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userEventScoreTopic(){
        return TopicBuilder
                .name(USER_EVENT_SCORE_TOPIC)
                .partitions()
                .replicas(replicas)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }

    @Bean
    public NewTopic leaderboardUpdateTopic(){
        return TopicBuilder
                .name(LEADERBOARD_UPDATE_TOPIC)
                .partitions()
                .replicas(replicas)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }
}
