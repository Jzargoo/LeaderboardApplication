package com.jzargo.usermicroservice.config;

import com.jzargo.messaging.FailedLeaderboardCreation;
import com.jzargo.messaging.UserAddedLeaderboard;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.Record;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
@EnableKafkaStreams
public class KafkaConfig {
    public static final String PULSE_LEADERBOARD
            = "pulse-leaderboard-topic";
    private static final String DEBEZIUM_FLC_TOPIC = "pgserver.users.public.failed_leaderboard_creation";
    private static final String DEBEZIUM_USERS_TOPIC = "pgserver.users.public.users";
    private static final String USER_UPDATING_TOPIC = "user-updating-topic";
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
                        (k,v) -> {
                            Map<String, Object> payload = (Map<String, Object>)v.get("payload");
                            if(payload == null) {
                                log.warn("Payload cannot be a null");
                                throw new RuntimeException("Incorrect message in debezium topic");
                            }

                            Map<String, Object> after = (Map<String, Object>) payload.get("after");
                            if(after == null) {
                                log.warn("After cannot be a null");
                                throw new RuntimeException("Incorrect message in debezium topic");
                            }

                            String leaderboardId = (String) after.get("leaderbaord_id");
                            String reason = (String) after.get("reason");
                            long userId = (long) after.get("user_id");
                            String sagaId = (String) after.get("saga_id");


                            FailedLeaderboardCreation failedLeaderboardCreation =
                                    new FailedLeaderboardCreation(leaderboardId, reason, userId);

                            return KeyValue.pair(sagaId,failedLeaderboardCreation);
                        }
                )
                .filter(
                        (k,v) -> {
                            if(
                                    v.getLbId() == null ||
                                            v.getReason().isBlank() ||
                                            v.getUserId() < 0 ||
                                            k == null ||
                                            k.isBlank()
                            ) {
                                log.warn("Incorrect message in debezium topic");
                                return false;
                            }
                            return true;
                        }
                )
                .process(
                        () -> (Processor<String, FailedLeaderboardCreation, String, FailedLeaderboardCreation>)
                                record -> {
                                    String id = UUID.randomUUID().toString();
                                    record.headers()
                                            .add(SAGA_ID_HEADER, record.key().getBytes())
                                            .add(MESSAGE_ID_HEADER, id.getBytes());
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
                DEBEZIUM_USERS_TOPIC,
                Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
        );

        stream
                .peek((k,v)-> log.debug("Caught event with key {}", k))
                .filter(
                        (k,v) -> {
                            Map<String, Object> payload = (Map<String, Object>)
                                    v.getOrDefault("payload", Map.of("op", "N"));

                            return payload != null &&
                                    "u".equals(payload.get("op"));
                        }
                )
                .map(
                        (k,v) -> {
                            Map<String, Object> payload = (Map<String, Object>)
                                    v.getOrDefault("payload", Map.of());
                            Map<String, Object> after = (Map<String, Object>)
                                    payload.getOrDefault("after", Map.of());
                            String sagaId = (String) after.get("saga_id");
                            Long userId = (Long) after.get("userId");
                            String lbId = (String) after.get("leaderboard_id");
                            return KeyValue.pair(sagaId, new UserAddedLeaderboard(
                                    lbId, userId
                            ));
                        }
                )
                .filter(
                        (k,v) ->
                                v.getLbId() != null &&
                                        !v.getLbId().isBlank() &&
                                        v.getUserId() != null &&
                                        v.getUserId() > 0 &&
                                        k != null &&
                                        !k.isBlank())
                .process(
                        () -> (Processor<String, UserAddedLeaderboard, String, UserAddedLeaderboard>)
                                record -> {
                                    String id = UUID.randomUUID().toString();
                                    record.headers()
                                            .add(SAGA_ID_HEADER, record.key().getBytes())
                                            .add(MESSAGE_ID_HEADER, id.getBytes());
                        }
                )
                .peek(
                        (k,v) -> log.info("Processed message with new key {} " +
                                "and value {} for topic user updates", k, v)
                )
                .to(
                        USER_UPDATING_TOPIC,
                        Produced.with(Serdes.String(), new JsonSerde<>(UserAddedLeaderboard.class))
                );
        return stream;
    }
}
