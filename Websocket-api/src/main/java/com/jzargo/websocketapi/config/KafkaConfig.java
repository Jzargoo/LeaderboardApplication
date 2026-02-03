package com.jzargo.websocketapi.config;

import com.jzargo.websocketapi.config.properties.KafkaPropertiesStorage;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic userEventScoreTopic(KafkaPropertiesStorage kafkaPropertiesStorage){
        return TopicBuilder
                .name(
                        kafkaPropertiesStorage
                                .getTopic().getNames()
                                .getUserEventScore()
                )
                .partitions()
                .partitions(kafkaPropertiesStorage.getTopic().getPartitions())
                .config("Min.insync.replicas",
                        String.valueOf(
                                kafkaPropertiesStorage.getTopic().getInSyncReplicas()
                        ))
                .build();
    }

    @Bean
    public NewTopic leaderboardUpdateTopic(KafkaPropertiesStorage kafkaPropertiesStorage){
        return TopicBuilder
                .name(kafkaPropertiesStorage
                        .getTopic().getNames()
                        .getLeaderboardUpdate()
                )
                .partitions(kafkaPropertiesStorage.getTopic().getPartitions())
                .replicas(kafkaPropertiesStorage.getTopic().getReplicas())
                .config("Min.insync.replicas",
                        String.valueOf(
                                kafkaPropertiesStorage.getTopic().getInSyncReplicas()
                        ))
                .build();
    }
}
