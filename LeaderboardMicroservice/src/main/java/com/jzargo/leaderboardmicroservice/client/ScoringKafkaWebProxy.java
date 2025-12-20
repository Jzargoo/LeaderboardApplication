package com.jzargo.leaderboardmicroservice.client;


import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
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

    @Override
    public void initiateEvents(LeaderboardEventInitialization leaderboardEventInitialization, String sagaId) {
        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaId,
                        leaderboardEventInitialization
                );

        String messageId = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, sagaId, messageId, sagaId);

        kafkaTemplate.send(record);
    }

    @Override
    public void deleteLeaderboardEvents(LeaderboardEventDeletion leaderboardEventDeletion, String sagaId) {
        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaId,
                        leaderboardEventDeletion
                );
        String s = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, sagaId,s,sagaId);
        kafkaTemplate.send(record);
    }
}
