package com.jzargo.leaderboardmicroservice.client;

import com.jzargo.leaderboardmicroservice.config.properties.KafkaPropertyStorage;
import com.jzargo.leaderboardmicroservice.saga.KafkaUtils;
import com.jzargo.messaging.DeleteLbEvent;
import com.jzargo.messaging.InitLeaderboardCreateEvent;
import com.jzargo.messaging.OutOfTimeEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;

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

    }

    @Override
    public void createLeaderboard(InitLeaderboardCreateEvent event, String sagaId) {
        ProducerRecord<String, Object> record = KafkaUtils.createRecord(
                kafkaPropertyStorage.getTopic().getNames().getLeaderboardEvent(),
                sagaId,
                event
        );

        KafkaUtils.addSagaHeaders(
                record, sagaId,
                kafkaPropertyStorage.getHeaders().getMessageId(),
                kafkaPropertyStorage.getHeaders().getSagaId()
        );
        kafkaTemplate.send(record);

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
    }
}
