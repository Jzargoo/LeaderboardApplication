package com.jzargo.scoringmicroservice.config;

import com.jzargo.messaging.FailedLeaderboardCreation;
import com.jzargo.messaging.UserScoreEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;

@Configuration
@Slf4j
@EnableKafkaStreams
public class KafkaConfig {
    public static final String COMMAND_STRING_SCORE_TOPIC = "user-string-score-command-topic";
    public static final String DEBEZIUM_SCORING_TOPIC = "pgserver.public.scoring_events";
    public static final String DEBEZIUM_FAILED_TOPIC = "pgserver.public.failed_create_leaderboard_events";
    public static final String SAGA_CREATE_LEADERBOARD_TOPIC = "saga-create-leaderboard-topic";
    public static final String USER_EVENT_SCORE_TOPIC = "user-event-score-topic";
    public static final String LEADERBOARD_EVENT_TOPIC = "leaderboard-event-topic";
    public static final String MESSAGE_ID = "message-id";
    public static final String GROUP_ID = "scoring-group";
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
                    String username = (String) after.get("username");
                    String region = (String) after.get("region");
                    Long userId = ((Number) after.get("user_id")).longValue();
                    double scoreDelta = ((Number) after.get("event_score")).doubleValue();

                    UserScoreEvent userScoreEvent = UserScoreEvent.builder()
                            .score(scoreDelta)
                            .userId(userId)
                            .lbId(lbId)
                            .username(username)
                            .region(region)
                            .build();

                    return new KeyValue<>(userId.toString(), userScoreEvent);
                })
                .filter((key, value) -> value != null)
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
                    Map<String, Object> payload = (Map<String, Object>) map.get("payload");
                    Map<String, Object> after = (Map<String, Object>) payload.get("after");
                    String leaderboardId = (String) after.get("leaderboard_id");
                    String reason = (String) after.get("reason");
                    // TODO : change table  add this columns
                    long userId = (long) after.get("user_id");
                    String sagaId = (String) after.get("saga_id");

                    FailedLeaderboardCreation failedLeaderboardCreation = new FailedLeaderboardCreation(leaderboardId, reason, userId);
                    return new KeyValue<>(sagaId, failedLeaderboardCreation);
                })
                .to(LEADERBOARD_EVENT_TOPIC, Produced.with(Serdes.String(), new JsonSerde<>(FailedLeaderboardCreation.class)));
        return stream;
    }
}
