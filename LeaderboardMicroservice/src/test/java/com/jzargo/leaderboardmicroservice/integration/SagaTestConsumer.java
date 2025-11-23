package com.jzargo.leaderboardmicroservice.integration;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@KafkaListener(topics = KafkaConfig.LEADERBOARD_UPDATE_TOPIC, groupId = "${kafka.consumer.group-id")
public class SagaTestConsumer {

        private final BlockingQueue<UserNewLeaderboardCreated>
                userNewLeaderboardCreatedBlockingQueue = new LinkedBlockingQueue<>();
        private final BlockingQueue<LeaderboardEventInitialization>
                leaderboardEventInitializationBlockingQueue = new LinkedBlockingQueue<>();

    public BlockingQueue<UserNewLeaderboardCreated> getUserNewLeaderboardCreatedBlockingQueue() {
        return userNewLeaderboardCreatedBlockingQueue;
    }

    public BlockingQueue<LeaderboardEventInitialization> getLeaderboardEventInitializationBlockingQueue() {
        return leaderboardEventInitializationBlockingQueue;
    }

    @KafkaHandler
    public void handleUserNewLeaderboardCreated(UserNewLeaderboardCreated event){
        userNewLeaderboardCreatedBlockingQueue.add(event);
    }
    @KafkaHandler
    public void handleLeaderboardEventInitialization(LeaderboardEventInitialization event){
        leaderboardEventInitializationBlockingQueue.add(event);
    }

}