package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FactoryScoringWebProxy {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    // Once upon a time I add other realizations
    public ScoringServiceWebProxy getClient(TypesOfProxy type) {
        return switch (type) {
            case REST -> null;
            default -> new ScoringKafkaWebProxy(kafkaTemplate, kafkaPropertyStorage);
        };
    }
}
