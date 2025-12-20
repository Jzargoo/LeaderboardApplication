package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserKafkaWebProxy implements UserServiceWebProxy {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void createUserAddedLeaderboard(UserNewLeaderboardCreated userNewLeaderboardCreated, String sagaId) {
        ProducerRecord<String, Object> record =
                SagaUtils.createRecord(
                        KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        sagaId,
                        userNewLeaderboardCreated);

        String messageId = SagaUtils.newMessageId();
        SagaUtils.addSagaHeaders(record, sagaId, messageId, sagaId);
        kafkaTemplate.send(record);
    }


}
