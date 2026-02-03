package com.jzargo.websocketapi.utils;

import com.jzargo.websocketapi.config.KafkaConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.support.KafkaHeaders;

import java.util.UUID;

public class KafkaSendUtils {
    public static String newMessageId(){
        return UUID.randomUUID().toString();
    }

    public static <K, V>ProducerRecord<K,V> buildProducerRecord(String topic, K key, V value){
        return new ProducerRecord<>(topic, key, value);
    }

    public static void addBasicHeaders(ProducerRecord<?,?> record){
        record.headers().add(KafkaConfig.MESSAGE_ID, newMessageId().getBytes())
                .add(KafkaHeaders.RECEIVED_KEY, record.key().toString().getBytes());
    }
}
