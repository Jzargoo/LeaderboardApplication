package com.jzargo.scoringmicroservice.config;

import com.jzargo.messaging.FailedLeaderboardCreation;
import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.UserScoreEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;
import java.util.UUID;

@Configuration
@Slf4j
@EnableKafkaStreams
public class KafkaConfig {
    public static final String COMMAND_STRING_SCORE_TOPIC = "user-string-score-command-topic";
    private static final String DEBEZIUM_SCORING_TOPIC = "pgserver.public.scoring_events";
    private static final String DEBEZIUM_FAILED_TOPIC = "pgserver.public.failed_create_leaderboard_events";
    private static final String DEBEZIUM_DELETED_EVENT_TOPIC = "pgserver.public.deleted_events";
    public static final String SAGA_CREATE_LEADERBOARD_TOPIC = "saga-create-leaderboard-topic";
    public static final String USER_EVENT_SCORE_TOPIC = "user-event-score-topic";
    public static final String LEADERBOARD_EVENT_TOPIC = "leaderboard-event-topic";
    public static final String MESSAGE_HEADER = "message-id";
    public static final String GROUP_ID = "scoring-group";
    public static final String SAGA_HEADER = "saga-id";

    @Bean
    public NewTopic userEventScoreTopic(){
        return TopicBuilder
                .name(USER_EVENT_SCORE_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic leaderboardEventTopic(){
        return TopicBuilder
                .name(LEADERBOARD_EVENT_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

    @Bean
    public NewTopic commandStringScoreTopic(){
        return TopicBuilder
                .name(COMMAND_STRING_SCORE_TOPIC)
                .partitions(3)
                .replicas(2)
                .config("Min.insync.replicas", "2")
                .build();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KStream<String, Map<String, Object>> KScoringStream(StreamsBuilder streamsBuilder) {

        JsonSerde<UserScoreEvent> userScoreSerde = new JsonSerde<>(UserScoreEvent.class);

        KStream<String, Map<String, Object>> stream = streamsBuilder
                .stream(
                        DEBEZIUM_SCORING_TOPIC,
                        Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
                        );

        stream
                .peek((key, value) -> log.info(
                        "Received message in scoring microservice " +
                                "from Debezium topic: {} with key: {}",
                        DEBEZIUM_SCORING_TOPIC, key))
                .filter((key, value) -> {
                    if (value == null) return false;
                    Map<String, Object> payload = (Map<String, Object>) value.get("payload");
                    return payload != null && "c".equals(payload.get("op"));
                })
                .map((key, value) -> {

                    Map<String, Object> payload = (Map<String, Object>) value.get("payload");
                    Map<String, Object> after = (Map<String, Object>) payload.get("after");

                    if (after == null) {
                        log.warn("Skipping event with null 'after' for key: {}", key);
                        return null;
                    }

                    String lbId = (String) after.get("lb_id");
                    Long userId = ((Number) after.get("user_id")).longValue();
                    double scoreDelta = ((Number) after.get("event_score")).doubleValue();

                    UserScoreEvent userScoreEvent = UserScoreEvent.builder()
                            .score(scoreDelta)
                            .userId(userId)
                            .lbId(lbId)
                            .build();

                    return new KeyValue<>(userId.toString(), userScoreEvent);
                })
                .filter((key, value) ->
                        value != null &&
                                value.getLbId() != null &&
                                !value.getLbId().isBlank() &&
                                value.getUserId() != null &&
                                value.getScore() != 0
                )
                .process(
                        () -> (Processor<String, UserScoreEvent, String, UserScoreEvent>)
                                record -> {
                                    String id = UUID.randomUUID().toString();
                                    record.headers()
                                            .add(SAGA_HEADER, record.key().getBytes())
                                            .add(MESSAGE_HEADER, id.getBytes());
                                }
                )
                .peek(
                        (key, value) -> log.info(
                                "Preparing to send message to topic: {} with key: {} and Value {}",
                                LEADERBOARD_EVENT_TOPIC, key, value)
                )
                .to(LEADERBOARD_EVENT_TOPIC,  Produced.with(Serdes.String(), userScoreSerde));
        return stream;
    }

    @Bean
    @SuppressWarnings("unchecked")
    public KStream<String, Map<String, Object>> KFailedLbEventsStream(StreamsBuilder streamsBuilder) {

        KStream<String, Map<String, Object>> stream = streamsBuilder
                .stream(
                        DEBEZIUM_FAILED_TOPIC,
                        Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
                );

        stream
                .peek((key, value) -> log.info(
                        "Received failed leaderboard event message with key: {} and value: {}",
                        key, value
                ))
                .filter((key, value) -> {
                    if (value == null) return false;
                    Map<String, Object> payload = (Map<String, Object>) value.get("payload");
                    return payload != null && "c".equals(payload.get("op"));
                })
                .map((key, map) -> {

                    Map<String, Object> payload = (Map<String, Object>) map.getOrDefault("payload", Map.of());
                    Map<String, Object> after = (Map<String, Object>) payload.getOrDefault("after", Map.of());

                    String leaderboardId = (String) after.get("leaderboard_id");
                    String reason = (String) after.get("reason");
                    long userId = (long) after.get("user_id");

                    String sagaId = (String) after.get("saga_id");

                    FailedLeaderboardCreation failedLeaderboardCreation = new FailedLeaderboardCreation(
                            leaderboardId, reason, userId, FailedLeaderboardCreation.SourceOfFail.EVENTS);
                    return new KeyValue<>(sagaId, failedLeaderboardCreation);
                })
                .filter(
                        (k,v) ->
                                v.getLbId() != null &&
                                        !v.getLbId().isBlank() &&
                                         v.getReason() !=null &&
                                        !v.getReason().isBlank() &&
                                         v.getUserId() != null &&
                                         v.getUserId() > 1
                )
                .process(
                        () -> (Processor<String, FailedLeaderboardCreation, String, FailedLeaderboardCreation>)
                                record -> {
                                    String id = UUID.randomUUID().toString();
                                    record.headers()
                                            .add(SAGA_HEADER, record.key().getBytes())
                                            .add(MESSAGE_HEADER, id.getBytes());
                                }
                )
                .to(LEADERBOARD_EVENT_TOPIC, Produced.with(Serdes.String(), new JsonSerde<>(FailedLeaderboardCreation.class)));
        return stream;
    }
    @Bean
    @SuppressWarnings("unchecked")
    public KStream<String, Map<String, Object>> KDeletedEvents(StreamsBuilder streamsBuilder) {

        KStream<String, Map<String, Object>> stream = streamsBuilder
                .stream(
                        DEBEZIUM_DELETED_EVENT_TOPIC,
                        Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
                );

        stream
                .peek((key, value) -> log.info(
                        "Received failed leaderboard event message with key: {}",
                        key
                ))
                .filter((key, value) -> {
                    if (value == null) return false;
                    Map<String, Object> payload = (Map<String, Object>) value.get("payload");
                    return payload != null && "c".equals(payload.get("op"));
                })
                .map((key, map) -> {

                    Map<String, Object> payload = (Map<String, Object>) map.getOrDefault("payload", Map.of());
                    Map<String, Object> after = (Map<String, Object>) payload.getOrDefault("after", Map.of());

                    String leaderboardId = (String) after.get("leaderboard_id");
                    String sagaId = (String) after.get("saga_id");

                    LeaderboardEventDeletion failedLeaderboardCreation =
                            new LeaderboardEventDeletion(leaderboardId);
                    return new KeyValue<>(sagaId, failedLeaderboardCreation);
                })
                .filter(
                        (k,v) ->
                                v.getLbId() != null &&
                                        k != null
                )
                .process(
                        () -> (Processor<String, LeaderboardEventDeletion, String, LeaderboardEventDeletion>)
                                record -> {
                                    String id = UUID.randomUUID().toString();
                                    record.headers()
                                            .add(SAGA_HEADER, record.key().getBytes())
                                            .add(MESSAGE_HEADER, id.getBytes());
                                }
                )
                .to(LEADERBOARD_EVENT_TOPIC, Produced.with(Serdes.String(), new JsonSerde<>(LeaderboardEventDeletion.class)));
        return stream;
    }
}