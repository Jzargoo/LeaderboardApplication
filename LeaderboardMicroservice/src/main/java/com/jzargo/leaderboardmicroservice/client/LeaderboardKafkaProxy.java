package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.messaging.DeleteLbEvent;
import com.jzargo.messaging.InitLeaderboardCreateEvent;
import com.jzargo.messaging.OutOfTimeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@RequiredArgsConstructor
public class LeaderboardKafkaProxy implements LeaderboardServiceWebProxy{

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaPropertyStorage kafkaPropertyStorage;

    @Override
    public void outOfTime(OutOfTimeEvent event, String sagaId) {
        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                sagaId,
                event
        );

        KafkaUtils.addSagaHeaders(
                record,
                sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(), 
                kafkaPropertyStorage.getHeaders().getSagaId()
                );

        kafkaTemplate.send(record);

        logPublishing(sagaId);
    }

    private static void logPublishing(String sagaId) {
        log.debug("The message was published with saga id: {}", sagaId);
    }

    @Override
    public void createLeaderboard(InitLeaderboardCreateEvent event, String sagaId) {
        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames().getSagaCreateLeaderboard(),
                sagaId,
                event
        );

        KafkaUtils.addSagaHeaders(
                record, sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                kafkaPropertyStorage.getHeaders().getSagaId()
        );
        kafkaTemplate.send(record);

        logPublishing(sagaId);

    }

    @Override
    public void compensateLeaderboard(DeleteLbEvent deleteLbEvent, String sagaId) {
        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                sagaId,
                deleteLbEvent
        );

        KafkaUtils.addSagaHeaders(
                record,
                sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                kafkaPropertyStorage.getHeaders().getSagaId()
        );

        kafkaTemplate.send(record);

        logPublishing(sagaId);

    }
}
