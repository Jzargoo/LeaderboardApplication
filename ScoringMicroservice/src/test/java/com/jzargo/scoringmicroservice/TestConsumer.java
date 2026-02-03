package com.jzargo.scoringmicroservice;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@KafkaListener(topics = "dbz.public.users", groupId = "scoring-service-group")
public class TestConsumer {
    private Map<String, Object> lastMessage;

    @KafkaListener(topics = "dbz.public.users", groupId = "scoring-service-group")
    public void listen(String key, Map<String, Object> message) {
        System.out.println("Received message: " + key);
        lastMessage = message;
    }

    public Map<String, Object> getLastMessage() {
        return lastMessage;
    }
}
