package com.jzargo.leaderboardmicroservice.client;


import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.LeaderboardEventInitialization;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ScoringKafkaWebProxy implements ScoringServiceWebProxy {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    @Override
    public void initiateEvents(LeaderboardEventInitialization leaderboardEventInitialization, String sagaId) {
        ProducerRecord<String, Object> record =
                KafkaUtils.createRecord(
                        kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                        sagaId,
                        leaderboardEventInitialization
                );

        KafkaUtils.addSagaHeaders(
                record,
                sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                kafkaPropertyStorage.getHeaders().getSagaId()
        );
        kafkaTemplate.send(record);
    }

    @Override
    public void deleteLeaderboardEvents(LeaderboardEventDeletion leaderboardEventDeletion, String sagaId) {
        ProducerRecord<String, Object> record =
                KafkaUtils.createRecord(
                        kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                        sagaId,
                        leaderboardEventDeletion
                );
        
        KafkaUtils.addSagaHeaders(
                record,
                sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                kafkaPropertyStorage.getHeaders().getSagaId()
        );
        
        kafkaTemplate.send(record);
    }
}
