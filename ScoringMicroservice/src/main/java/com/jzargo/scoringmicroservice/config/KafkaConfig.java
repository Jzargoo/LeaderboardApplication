package com.jzargo.scoringmicroservice.config;

import com.jzargo.messaging.FailedLeaderboardCreation;
import com.jzargo.messaging.LeaderboardEventDeletion;
import com.jzargo.messaging.UserScoreEvent;
import com.jzargo.scoringmicroservice.config.properties.KafkaPropertiesStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.processor.api.Processor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Configuration
@EnableKafkaStreams
@RequiredArgsConstructor
@EnableConfigurationProperties
public class KafkaConfig {

    private final KafkaPropertiesStorage kafkaPropertiesStorage;
    @Bean
    public NewTopic userEventScoreTopic(){
        return TopicBuilder
                .name(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getCommandStringScore()
                )
                .partitions(
                        kafkaPropertiesStorage.getTopic().getPartitions()
                )
                .replicas(
                        kafkaPropertiesStorage.getTopic().getReplicas()
                )
                .config("Min.insync.replicas",
                        kafkaPropertiesStorage.getTopic().getInsyncReplicas().toString()
                )
                .build();
    }

    //Topic with events which leaderboard read
    @Bean
    public NewTopic leaderboardEventTopic(){
        return TopicBuilder
                .name(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getLeaderboardEvent()
                )
                .partitions(
                        kafkaPropertiesStorage.getTopic().getPartitions()
                )
                .replicas(
                        kafkaPropertiesStorage.getTopic().getReplicas()
                )
                .config("Min.insync.replicas",
                        kafkaPropertiesStorage.getTopic().getInsyncReplicas().toString()
                )
                .build();
    }

    //Topic for scoring microservice for mapping string event -> increasedValue
    @Bean
    public NewTopic commandStringScoreTopic(){
        return TopicBuilder
                .name(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getCommandStringScore()
                )
                .partitions(
                        kafkaPropertiesStorage.getTopic().getPartitions()
                )
                .replicas(
                        kafkaPropertiesStorage.getTopic().getReplicas()
                )
                .config("Min.insync.replicas",
                        kafkaPropertiesStorage.getTopic().getInsyncReplicas().toString()
                )
                .build();
    }

    @Bean
    public KStream<String, Map<String, Object>> KScoringStream(StreamsBuilder streamsBuilder) {

        JsonSerde<UserScoreEvent> userScoreSerde = new JsonSerde<>(UserScoreEvent.class);

        KStream<String, Map<String, Object>> stream = streamsBuilder
                .stream(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getDebeziumScoring(),
                        Consumed.with(Serdes.String(), new JsonSerde<>(Map.class))
                        );

        stream
                .peek(
                        (key, value) -> log.info(
                                "Received message in scoring microservice " +
                                        "from Debezium topic: {} with key: {}",
                                kafkaPropertiesStorage.getTopic().getNames()
                                        .getDebeziumScoring(),
                                key
                        )
                )
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
                                            .add(
                                                    KafkaHeaders.RECEIVED_KEY,
                                                    record.key().getBytes()
                                            )
                                            .add(
                                                    kafkaPropertiesStorage.getHeaders()
                                                            .getMessageId(),
                                                    id.getBytes()
                                            );
                                }
                )
                .peek(
                        (key, value) -> log.info(
                                "Preparing to send message to topic: {} with key: {} and Value {}",
                                kafkaPropertiesStorage.getTopic().getNames()
                                        .getLeaderboardEvent(),
                                key, value)
                )
                .to(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getLeaderboardEvent(),
                        Produced.with(Serdes.String(), userScoreSerde)
                );
        return stream;
    }

    @Bean
    public KStream<String, Map<String, Object>> KFailedLbEventsStream(StreamsBuilder streamsBuilder) {

        KStream<String, Map<String, Object>> stream = streamsBuilder
                .stream(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getDebeziumFailed(),
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
                                            .add(
                                                    kafkaPropertiesStorage.getHeaders().getSagaId(),
                                                    record.key().getBytes()
                                            )
                                            .add(
                                                    KafkaHeaders.RECEIVED_KEY,
                                                    record.key().getBytes()
                                            )
                                            .add(kafkaPropertiesStorage.getHeaders()
                                                    .getMessageId(),
                                                    id.getBytes()
                                            );
                                }
                )
                .to(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getSagaCreateLeaderboard(),
                        Produced.with(Serdes.String(), new JsonSerde<>(FailedLeaderboardCreation.class))
                );
        return stream;
    }
    @Bean
    public KStream<String, Map<String, Object>> KDeletedEvents(StreamsBuilder streamsBuilder) {

        KStream<String, Map<String, Object>> stream = streamsBuilder
                .stream(
                        kafkaPropertiesStorage.getTopic().getNames()
                                .getDebeziumDeleted(),
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
                                            .add(
                                                    kafkaPropertiesStorage.getHeaders().getSagaId(),
                                                    record.key().getBytes()
                                            )
                                            .add(
                                                    KafkaHeaders.RECEIVED_KEY,
                                                    record.key().getBytes()
                                            )
                                            .add(kafkaPropertiesStorage.getHeaders()
                                                            .getMessageId(),
                                                    id.getBytes()
                                            );
                                }
                )
                .to(
                        kafkaPropertiesStorage.getTopic()
                                .getNames().getSagaCreateLeaderboard(),
                        Produced.with(Serdes.String(), new JsonSerde<>(LeaderboardEventDeletion.class)));
        return stream;
    }
}