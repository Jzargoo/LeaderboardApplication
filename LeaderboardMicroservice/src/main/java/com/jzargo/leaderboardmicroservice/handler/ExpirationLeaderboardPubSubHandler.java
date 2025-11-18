package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.config.KafkaConfig;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaUtils;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ExpirationLeaderboardPubSubHandler implements MessageListener {
    private final LeaderboardInfoRepository leaderboardInfoRepository;
    private final LeaderboardService leaderboardService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ExpirationLeaderboardPubSubHandler(
            LeaderboardInfoRepository leaderboardInfoRepository,
            LeaderboardService leaderboardService,
            KafkaTemplate<String, Object> kafkaTemplate) {

        this.leaderboardInfoRepository = leaderboardInfoRepository;
        this.leaderboardService = leaderboardService;
        this.kafkaTemplate = kafkaTemplate;
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
            leaderboardService.deleteLeaderboard(byId.getKey());
        } else {
            log.debug("Leaderboard expired");
            sendKafkaMessage();
        }
    }

    private void sendKafkaMessage(String lbId) {
        SagaUtils.createRecord(
                KafkaConfig.LEADERBOARD_UPDATE_TOPIC,
                lbId,

        );
        kafkaTemplate.send()
    }

}
