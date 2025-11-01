package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.mapper.CreateInitialCreateLeaderboardSagaRequestMapper;
import com.jzargo.leaderboardmicroservice.repository.sagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import com.jzargo.messaging.LeaderboardEventInitialization;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@KafkaListener(
        topics = {KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC},
        groupId = KafkaConfig.GROUP_ID
)
public class SagaLeaderboardCreateImpl implements SagaLeaderboardCreate{
    private final CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final sagaControllingStateRepository sagaControllingStateRepository;
    private final LeaderboardService leaderboardService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisScript<String> sagaSuccessfulScript;

    public SagaLeaderboardCreateImpl(
            CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper,
            KafkaTemplate<String, Object> kafkaTemplate,
            sagaControllingStateRepository sagaControllingStateRepository, LeaderboardService leaderboardService,
            StringRedisTemplate stringRedisTemplate, RedisScript<String> sagaSuccessfulScript){
        this.createInitialCreateLeaderboardSagaRequestMapper = createInitialCreateLeaderboardSagaRequestMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.sagaControllingStateRepository = sagaControllingStateRepository;
        this.leaderboardService = leaderboardService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.sagaSuccessfulScript = sagaSuccessfulScript;
    }

    @Override
    @Transactional
    public void startSaga(CreateLeaderboardRequest request, long userId, String username, String region) {
        InitLeaderboardCreateEvent map = createInitialCreateLeaderboardSagaRequestMapper.map(request);
        map.setOwnerId(userId);
        map.setUsername(username);
        SagaControllingState build = SagaControllingState.builder()
                .id(UUID.randomUUID().toString())
                .status(SagaStep.INITIATED)
                .lastStepCompleted("")
                .build();

        sagaControllingStateRepository.save(build);

        ProducerRecord<String, Object> record =
                new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC, region, map);

        record.headers()
                .add(KafkaConfig.MESSAGE_ID, UUID.randomUUID().toString().getBytes())
                .add(KafkaConfig.SAGA_ID_HEADER, build.getId().getBytes())
                .add(KafkaHeaders.RECEIVED_KEY, region.getBytes());
        kafkaTemplate.send(record);
    }

    @KafkaHandler
    @Transactional
    public void handleCreateLeaderboardSaga(InitLeaderboardCreateEvent event,
                                            @Header(KafkaConfig.SAGA_ID_HEADER) String sagaId,
                                            @Header(KafkaConfig.MESSAGE_ID) String messageId,
                                            @Header(KafkaHeaders.RECEIVED_KEY) String region
                                            ){
        String key = "processed:" + messageId;
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.warn("Handled processed message with id {}", messageId);
            return;
        }
        try{
            leaderboardService.createLeaderboard(event, region);
            stringRedisTemplate.execute(
                    sagaSuccessfulScript, List.of("saga_controlling_state"+ ":" + sagaId),
                    event.getLbId(), SagaStep.LEADERBOARD_CREATED, SagaStep.INITIATED, ""
            );
            ProducerRecord<String, Object> record;
            if (event.isMutable()) {
                LeaderboardEventInitialization build =
                        LeaderboardEventInitialization.builder()
                                .lbId(event.getLbId())
                                .events(event.getEvents())
                                .isPublic(event.isPublic())
                                .metadata(
                                    Map.of("expire_date", event.getExpireAt())
                                )
                                .build();
                record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        event.getLbId(), build);
            } else {
                UserNewLeaderboardCreated userNewLeaderboardCreated = new UserNewLeaderboardCreated(
                        event.getLbId(), event.getNameLb(),
                        event.getOwnerId(), event.getDescription()
                );
                record = new ProducerRecord<>(KafkaConfig.SAGA_CREATE_LEADERBOARD_TOPIC,
                        event.getLbId(), userNewLeaderboardCreated);
            }

            record.headers()
                    .add(KafkaConfig.MESSAGE_ID, UUID.randomUUID().toString().getBytes())
                    .add(KafkaConfig.SAGA_ID_HEADER, sagaId.getBytes())
                    .add(KafkaHeaders.RECEIVED_KEY, event.getLbId().getBytes());
            kafkaTemplate.send(record);

            log.info("Processed saga create leaderboard with id {}", sagaId);
        }catch (Exception e){
            log.error("Error while processed message {}", messageId,e);
            stringRedisTemplate.delete(key);
        }
    }
}