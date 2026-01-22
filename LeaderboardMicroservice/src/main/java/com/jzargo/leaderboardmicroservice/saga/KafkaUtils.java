package com.jzargo.leaderboardmicroservice.saga;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class KafkaUtils {
    private KafkaUtils() {}

    public static final Duration DEFAULT_IDEMPOTENCY_TTL = Duration.ofDays(6);

    public static boolean tryAcquireProcessingLock(StringRedisTemplate redis, String messageId, Duration ttl) {
        String key = "processed:" + messageId;
        Boolean res = redis.opsForValue().setIfAbsent(key, "0", ttl);
        return Boolean.TRUE.equals(res);
    }

    public static boolean tryAcquireProcessingLock(StringRedisTemplate redis, String messageId) {
        return tryAcquireProcessingLock(redis, messageId, DEFAULT_IDEMPOTENCY_TTL);
    }

    public static void releaseProcessingLock(StringRedisTemplate redis, String messageId) {
        String key = "processed:" + messageId;
        try {
            redis.delete(key);
        } catch (Exception ignored) {}
    }
    public static void addSagaHeaders(ProducerRecord<String, Object> record,
                                      String sagaId,
                                      String partitionKey,
                                      String messageIdHeader, String sagaIdHeader) {
        record.headers()
                .add(messageIdHeader, newMessageId().getBytes(StandardCharsets.UTF_8))
                .add(sagaIdHeader, sagaId.getBytes(StandardCharsets.UTF_8))
                .add(KafkaHeaders.RECEIVED_KEY, partitionKey.getBytes(StandardCharsets.UTF_8));

    }
    public static void addSagaHeaders(ProducerRecord<String, Object> record,
                                      String sagaId,
                                      String messageIdHeader, String sagaIdHeader) {
        addSagaHeaders(
                record,
                sagaId,
                sagaId,
                messageIdHeader,
                sagaIdHeader);
    }

    public static ProducerRecord<String, Object> createRecord(String topic, String partitionKey, Object payload) {
        return new ProducerRecord<>(topic, partitionKey, payload);
    }

    public static String newMessageId() {
        return UUID.randomUUID().toString();
    }

    public static void addCommonHeaders(ProducerRecord<String, Object> record, String key, String messageIdHeader) {
        addCommonHeaders(record, key, messageIdHeader, newMessageId());
    }

    public static void addCommonHeaders(ProducerRecord<String, Object> record, String key, String messageIdHeader, String messageId) {
        record.headers().add(messageIdHeader, messageId.getBytes())
                .add(KafkaHeaders.RECEIVED_KEY, key.getBytes());
    }
}
