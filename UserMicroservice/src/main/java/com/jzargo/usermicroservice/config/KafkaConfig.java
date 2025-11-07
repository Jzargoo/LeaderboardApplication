package com.jzargo.usermicroservice.config;

import com.jzargo.messaging.FailedLeaderboardCreation;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;

@Slf4j
@Configuration
@EnableKafkaStreams
public class KafkaConfig {
    public static final String PULSE_LEADERBOARD
            = "pulse-leaderboard-topic";
    private static final String DEBEZIUM_FLC_TOPIC = "pgserver.users.public.failed_leaderboard_creation";
    public static final String SAGA_CREATE_LEADERBOARD_TOPIC = "saga-create-leaderboard-topic";
    public static final String GROUP_ID = "users-group" ;
    public static final String MESSAGE_ID_HEADER = "message-id";
    public static final String SAGA_ID_HEADER = "saga-id";
    @Value("${kafka.topic.partition.count}")
    private Integer partitionCount;
    @Value("${kafka.topic.replicas}")
    private Integer replicasCount;
    @Value("${kafka.topic.partition.insync-replicas}")
    private Integer minInSyncReplicas;

    @Bean
    NewTopic pulseLeaderboard(){
        return TopicBuilder
                .name(PULSE_LEADERBOARD)
                .partitions(partitionCount)
                .replicas(replicasCount)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }

    @Bean
    NewTopic sagaCreateLeaderboardTopic(){
        return TopicBuilder
                .name(SAGA_CREATE_LEADERBOARD_TOPIC)
                .partitions(partitionCount)
                .replicas(replicasCount)
                .config("Min.insync.replicas", String.valueOf(minInSyncReplicas))
                .build();
    }


    @Bean
    @SuppressWarnings("unchecked")
    public KStream<String, Map<String, Object>> failedLeaderboardCreation(StreamsBuilder streamsBuilder){
        KStream<String, Map<String, Object>> stream = streamsBuilder.stream(
                DEBEZIUM_FLC_TOPIC,
                Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
        );

        stream
                .peek((k,v) -> log.debug("Caught failed attempt to create leaderboard with key {} and value {}", k, v))
                .filter((key, value) -> {
                    if (value == null) return false;
                    Map<String, Object> payload = (Map<String, Object>) value.get("payload");
                    return payload != null && "c".equals(payload.get("op"));
                })
                .map(
                        // TODO : simplify add filter with validation
                        (k,v) -> {
                            Map<String, Object> payload = (Map<String, Object>)v.get("payload");
                            if(payload == null) {
                                log.warn("Incorrect message in debezium topic");
                                throw new RuntimeException("Incorrect message in debezium topic");
                            }

                            Map<String, Object> after = (Map<String, Object>) payload.get("after");
                            if(after == null) {
                                log.warn("Incorrect message in debezium topic");
                                throw new RuntimeException("Incorrect message in debezium topic");
                            }

                            String leaderboardId = (String) after.get("leaderbaord_id");
                            String reason = (String) after.get("reason");
                            long userId = (long) after.get("user_id");
                            String sagaId = (String) after.get("saga_id");

                            if(
                                    leaderboardId == null ||
                                            leaderboardId.isBlank() ||
                                            userId < 0 ||
                                            sagaId == null ||
                                            sagaId.isBlank()
                            ) {
                                log.warn("Incorrect message in debezium topic");
                                throw new RuntimeException("Incorrect message in debezium topic");
                            }

                            FailedLeaderboardCreation failedLeaderboardCreation =
                                    new FailedLeaderboardCreation(leaderboardId, reason, userId);

                            return KeyValue.pair(sagaId,failedLeaderboardCreation);
                        }
                )
                .peek((k,v) ->
                        log.info("Successfully processed message with key(sagaId) {}", k))
                .to(
                        SAGA_CREATE_LEADERBOARD_TOPIC,
                        Produced.with(Serdes.String(), new JsonSerde<>(FailedLeaderboardCreation.class))
                );
        return stream;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KStream<String, Map<String, Object>> userUpdate(StreamsBuilder streamsBuilder){
        KStream<String, Map<String, Object>> stream = streamsBuilder.stream(
                DEBEZIUM_FLC_TOPIC,
                Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
        );

        return stream;
    }

}
