package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.DiedLeaderboardEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@Slf4j
public class ExpirationLeaderboardPubSubHandler implements MessageListener {
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;
    private final StringRedisTemplate stringRedisTemplate;

    public ExpirationLeaderboardPubSubHandler(
            LeaderboardInfoRepository leaderboardInfoRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            SagaLeaderboardCreate sagaLeaderboardCreate,
            StringRedisTemplate stringRedisTemplate) {

        this.leaderboardInfoRepository = leaderboardInfoRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String expiredKey = message.toString();

        if(
                expiredKey.isBlank() &&
                !expiredKey.startsWith("leaderboard_signal")
        ) {
            log.debug("Message with uninteresting or blank key is received");
            return;
        }

        log.debug("leaderboard expiration key is caught");

        LeaderboardInfo byId = leaderboardInfoRepository
                .findById(expiredKey.split(":")[1])
                .orElseThrow();

        if(!byId.isActive()) {
            boolean b = sagaLeaderboardCreate.stepOutOfTime(byId.getId());
            if (!b) {
                log.debug("Leaderboard was not compensated because it done well");
            }
        } else {
            log.debug("Leaderboard with id {} expired", byId.getId());

            Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                    .reverseRangeWithScores(byId.getKey(), 0, -1);

            Map<Long, Double> collected = Map.of();

            if (typedTuples != null) {
                collected = typedTuples.stream().map(
                        tuple ->
                                Map.entry(
                                        Long.valueOf(
                                                Objects.requireNonNull(tuple.getValue())
                                        ),
                                        Objects.requireNonNull(tuple.getScore()))
                ).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
            } else{
                log.warn("Empty leaderboard");
            }
            sendKafkaMessage(byId.getId(), byId.getName(), byId.getDescription(), collected);
        }
    }

    private void sendKafkaMessage(String lbId, String lbName, String lbDescription, Map<Long, Double> collected) {
        DiedLeaderboardEvent diedLeaderboardEvent = new DiedLeaderboardEvent(lbName, lbDescription, collected);

        ProducerRecord<String, Object> record = SagaUtils.createRecord(
                KafkaConfig.LEADERBOARD_UPDATE_TOPIC,
                lbId,
                diedLeaderboardEvent
        );

        SagaUtils.addSagaHeaders(
                record, "",
                SagaUtils.newMessageId(), lbId);

        kafkaTemplate.send(record);
    }
}