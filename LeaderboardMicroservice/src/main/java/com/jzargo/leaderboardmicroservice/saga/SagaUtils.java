package com.jzargo.leaderboardmicroservice.saga;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class SagaUtils {
    private SagaUtils() {}

    public static final Duration DEFAULT_IDEMPOTENCY_TTL = Duration.ofDays(7);

    public static boolean tryAcquireProcessingLock(StringRedisTemplate redis, String messageId, Duration ttl) {
        String key = "processed:" + messageId;
        Boolean res = redis.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(res);
    }

    public static boolean tryAcquireProcessingLock(StringRedisTemplate redis, String messageId) {
        return tryAcquireProcessingLock(redis, messageId, DEFAULT_IDEMPOTENCY_TTL);
    }

    public static void releaseProcessingLock(StringRedisTemplate redis, String messageId) {
        String key = "processed:" + messageId;
        try {
            redis.delete(key);
        } catch (Exception ignored) {
        }
    }

    public static void addSagaHeaders(ProducerRecord<String, Object> record,
                                      String sagaId,
                                      String messageId,
                                      String partitionKey) {
        record.headers()
                .add("message_id", messageId.getBytes(StandardCharsets.UTF_8))
                .add("saga_id", sagaId.getBytes(StandardCharsets.UTF_8))
                .add(KafkaHeaders.RECEIVED_KEY, (partitionKey != null ? partitionKey : sagaId).getBytes(StandardCharsets.UTF_8));
    }

    public static ProducerRecord<String, Object> createRecord(String topic, String partitionKey, Object payload) {
        return new ProducerRecord<>(topic, partitionKey, payload);
    }

    public static String newMessageId() {
        return UUID.randomUUID().toString();
    }
}
