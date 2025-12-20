package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
import com.jzargo.messaging.DeleteLbEvent;
import com.jzargo.messaging.OutOfTimeEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class LeaderboardKafkaProxy implements LeaderboardServiceWebProxy{

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void outOfTime(OutOfTimeEvent event, String sagaId) {
        ProducerRecord<String, Object> record = SagaUtils.createRecord(
                KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                sagaId,
                event
        );

        SagaUtils.addSagaHeaders(record, sagaId, SagaUtils.newMessageId(), sagaId);
        kafkaTemplate.send(record);

    }

    @Override
    public void createLeaderboard(InitLeaderboardCreateEvent event, String sagaId) {
        ProducerRecord<String, Object> record = SagaUtils.createRecord(
                KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                sagaId,
                event
        );

        SagaUtils.addSagaHeaders(record, sagaId, SagaUtils.newMessageId(), sagaId);
        kafkaTemplate.send(record);

    }

    @Override
    public void compensateLeaderboard(DeleteLbEvent deleteLbEvent, String sagaId) {
        ProducerRecord<String, Object> record = SagaUtils.createRecord(
                KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                sagaId,
                deleteLbEvent
        );

        SagaUtils.addSagaHeaders(record, sagaId, SagaUtils.newMessageId(), sagaId);
        kafkaTemplate.send(record);
    }
}
