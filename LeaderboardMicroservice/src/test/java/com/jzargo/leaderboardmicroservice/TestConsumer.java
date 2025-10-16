package com.jzargo.leaderboardmicroservice;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserLocalUpdateEvent;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@KafkaListener(topics = KafkaConfig.LEADERBOARD_UPDATE_TOPIC, groupId = "${kafka.consumer.group-id")
public class TestConsumer {

    private final BlockingQueue<GlobalLeaderboardEvent> globalRecordsQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<UserLocalUpdateEvent> localRecordsQueue = new LinkedBlockingQueue<>();

    public BlockingQueue<GlobalLeaderboardEvent> getGlobalRecordsQueue() {
        return globalRecordsQueue;
    }

    public BlockingQueue<UserLocalUpdateEvent> getLocalRecordsQueue() {
        return localRecordsQueue;
    }

    @KafkaHandler
    void listenForGlobalEvents(GlobalLeaderboardEvent event) {
        globalRecordsQueue.add(event);
    }

    @KafkaHandler
    void listenForLocalEvents(UserLocalUpdateEvent event) {
        localRecordsQueue.add(event);
    }


}
