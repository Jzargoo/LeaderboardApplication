package com.jzargo.leaderboardmicroservice.handler;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import static org.springframework.data.redis.listener.Topic.pattern;

@Component
@Slf4j
public class RedisLocalLeaderboardHandler implements MessageListener {
    @PostConstruct
    public void subscription(RedisMessageListenerContainer container) {
        container.addMessageListener(this, pattern("leaderboard-cache:*:local-leaderboard-update"));
        log.trace("Subscribed to local-leaderboard-update channel");

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());

        log.info("Received local leaderboard update message: {}", body);
    }
}
