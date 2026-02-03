package com.jzargo.leaderboardmicroservice.config;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaPropertyStorage.class)
@Profile("!standalone")
public class KafkaConfig{

    private final KafkaPropertyStorage kafkaPropertyStorage;

    public KafkaConfig(KafkaPropertyStorage kafkaPropertyStorage) {
        this.kafkaPropertyStorage = kafkaPropertyStorage;
    }

    @Bean
    public NewTopic leaderboardEventsTopic(){
        return TopicBuilder
                .name(kafkaPropertyStorage
                        .getTopic().getNames()
                        .getLeaderboardEvent()
                )
                .partitions(kafkaPropertyStorage.getTopic().getPartitions())
                .replicas(kafkaPropertyStorage.getTopic().getReplicas())
                .config("min.insync.replicas",
                        String.valueOf(kafkaPropertyStorage.getTopic().getInSyncReplicas()))
                .build();
    }



    @Bean
    public NewTopic leaderboardUpdateTopic(){
        return TopicBuilder
                .name(kafkaPropertyStorage
                        .getTopic().getNames()
                        .getLeaderboardUpdateState()
                )
                .partitions(kafkaPropertyStorage.getTopic().getPartitions())
                .replicas(kafkaPropertyStorage.getTopic().getReplicas())
                .config("min.insync.replicas",
                        String.valueOf(kafkaPropertyStorage.getTopic().getInSyncReplicas()))
                .build();
    }
    @Bean
    public NewTopic sagaLeaderboardCreateTopic(){
        return TopicBuilder
                .name(kafkaPropertyStorage
                        .getTopic().getNames()
                        .getSagaCreateLeaderboard()
                )
                .partitions(kafkaPropertyStorage.getTopic().getPartitions())
                .replicas(kafkaPropertyStorage.getTopic().getReplicas())
                .config("min.insync.replicas",
                        String.valueOf(kafkaPropertyStorage.getTopic().getInSyncReplicas()))
                .build();
    }
}
