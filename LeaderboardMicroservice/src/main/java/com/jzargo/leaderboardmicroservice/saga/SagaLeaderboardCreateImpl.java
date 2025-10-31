package com.jzargo.leaderboardmicroservice.saga;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.core.messaging.InitLeaderboardCreateEvent;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.entity.SagaStep;
import com.jzargo.leaderboardmicroservice.mapper.CreateInitialCreateLeaderboardSagaRequestMapper;
import com.jzargo.leaderboardmicroservice.repository.sagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
@KafkaListener
public class SagaLeaderboardCreateImpl implements SagaLeaderboardCreate{
    private final CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper;
    private final KafkaTemplate<String, InitLeaderboardCreateEvent> kafkaTemplate;
    private final sagaControllingStateRepository sagaControllingStateRepository;
    private final LeaderboardService leaderboardService;
    private final StringRedisTemplate stringRedisTemplate;

    public SagaLeaderboardCreateImpl(CreateInitialCreateLeaderboardSagaRequestMapper createInitialCreateLeaderboardSagaRequestMapper, KafkaTemplate<String, InitLeaderboardCreateEvent> kafkaTemplate, sagaControllingStateRepository sagaControllingStateRepository, LeaderboardService leaderboardService, StringRedisTemplate stringRedisTemplate) {
        this.createInitialCreateLeaderboardSagaRequestMapper = createInitialCreateLeaderboardSagaRequestMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.sagaControllingStateRepository = sagaControllingStateRepository;
        this.leaderboardService = leaderboardService;
        this.stringRedisTemplate = stringRedisTemplate;
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
                .build();

        sagaControllingStateRepository.save(build);

        ProducerRecord<String, InitLeaderboardCreateEvent> record =
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
        Boolean success = stringRedisTemplate
                .opsForValue()
                .setIfAbsent("processed:" + messageId, "1", Duration.ofDays(7));
        if(success != null && !success) {
            log.warn("Handled processed message with id {}", messageId);
            return;
        }
        leaderboardService.createLeaderboard(event, region);
        SagaControllingState sagaControllingState = sagaControllingStateRepository
                .findById(sagaId)
                .orElseThrow();
        sagaControllingState.setStatus(
                SagaStep.LEADERBOARD_CREATED);
        sagaControllingState.setLeaderboardId(event.getLbId());
        sagaControllingStateRepository.save(sagaControllingState);
        sagaControllingState.setLastStepCompleted(SagaStep.INITIATED.name());
        log.info("Processed saga create leaderboard with id {}", sagaId);
    }
}
