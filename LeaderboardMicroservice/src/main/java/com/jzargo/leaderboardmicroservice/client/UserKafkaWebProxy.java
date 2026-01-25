package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserKafkaWebProxy implements UserServiceWebProxy {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    @Override
    public void createUserAddedLeaderboard(UserNewLeaderboardCreated userNewLeaderboardCreated, String sagaId) {
        ProducerRecord<String, Object> record =
                KafkaUtils.createRecord(
                        kafkaPropertyStorage.getTopic().getNames().getSagaCreateLeaderboard(),
                        sagaId,
                        userNewLeaderboardCreated);

        KafkaUtils.addSagaHeaders(
                record,
                sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                kafkaPropertyStorage.getHeaders().getSagaId()
        );
        kafkaTemplate.send(record);

        log.debug("The message was published with saga id: {}", sagaId);
    }


}
