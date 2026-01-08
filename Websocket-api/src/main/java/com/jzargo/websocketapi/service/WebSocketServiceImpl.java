package com.jzargo.websocketapi.service;

import com.jzargo.messaging.GlobalLeaderboardEvent;
import com.jzargo.messaging.UserEventHappenedCommand;
import com.jzargo.messaging.UserLocalUpdateEvent;
import com.jzargo.messaging.UserScoreUploadEvent;
import com.jzargo.websocketapi.config.KafkaConfig;
import com.jzargo.websocketapi.dto.InitUserScoreRequest;
import com.jzargo.websocketapi.dto.LeaderboardPushEvent;
import com.jzargo.websocketapi.dto.LeaderboardResponsePayload;
import com.jzargo.websocketapi.lifecylce.PropertiesStorage;
import com.jzargo.websocketapi.utils.KafkaSendUtils;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final LeaderboardWebClient leaderboardWebClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final PropertiesStorage propertiesStorage;

    @Override
    public void initLeaderboardScore(String id) {
        try {

            leaderboardWebClient.initUserScore(new InitUserScoreRequest(id));
            log.info("User successfully sent join event");
        } catch (FeignException.FeignServerException e) {
            log.error("Internal error in leaderboard microservice", e);

        } catch (FeignException.FeignClientException e) {
            log.error("Incorrect request to leaderboard microservice", e);
        }
    }

    @Override
    public void updateUserScore(String id, LeaderboardPushEvent.UpdateLeaderboardScore updateUserScore) {
        log.debug("Caught updateUserScore event for user id: {}", updateUserScore.getUserId());

        ProducerRecord<String, Object> uus =
                KafkaSendUtils.buildProducerRecord(KafkaConfig.USER_EVENT_SCORE_TOPIC,
                        id,
                        new UserScoreUploadEvent(
                                id,
                                updateUserScore.getUserId(),
                                updateUserScore.getNewScore(),
                                Map.of()
                        )
                );

        KafkaSendUtils.addBasicHeaders(uus);

        kafkaTemplate.send(uus);
    }

    @Override
    public void increaseUserScore(String id, LeaderboardPushEvent.IncreaseUserScore increaseUserScore) {
        log.debug("Caught  event IncreaseUserScore for user id: {}", increaseUserScore.getUserId());

        ProducerRecord<String, Object> ius =
                KafkaSendUtils.buildProducerRecord(
                        KafkaConfig.USER_EVENT_SCORE_TOPIC,
                        id,
                        new UserEventHappenedCommand(
                                id,
                                increaseUserScore.getEvent(),
                                increaseUserScore.getUserId(),
                                Map.of()
                        )
                );

        KafkaSendUtils.addBasicHeaders(ius);

        kafkaTemplate.send(ius);
    }

    @Override
    public void refreshLocalLeaderboard(UserLocalUpdateEvent userLocalUpdateEvent) {
        if(userLocalUpdateEvent == null ||
                userLocalUpdateEvent.getEntries() == null ||
                userLocalUpdateEvent.getEntries().isEmpty()){
            log.debug("No entries to send for local leaderboard id: {}",
                    userLocalUpdateEvent != null ? userLocalUpdateEvent.getLeaderboardId() : "null");
            return;
        }
        for(UserLocalUpdateEvent.UserLocalEntry entry: userLocalUpdateEvent.getEntries()){
            simpMessagingTemplate.convertAndSendToUser(
                    String.valueOf(entry.getUserId()),

                    propertiesStorage.getLocalPushEndpointPattern() +
                            userLocalUpdateEvent.getLeaderboardId()
                    ,

                    new LeaderboardResponsePayload(
                            userLocalUpdateEvent.getLeaderboardId(),
                            entry.getUserId(),
                            entry.getScore(),
                            entry.getRank()
                    )
            );
            log.trace("Successfully sent local leaderboard update to user id: {}", entry.getUserId());
        }
        log.debug("Successfully sent local leaderboard update to lbId: {}",
                userLocalUpdateEvent.getLeaderboardId());
    }

    @Override
    public void refreshGlobalLeaderboard(GlobalLeaderboardEvent globalLeaderboardEvent) {
        if(globalLeaderboardEvent == null ||
                globalLeaderboardEvent.getTopNLeaderboard() == null ||
                globalLeaderboardEvent.getTopNLeaderboard().isEmpty()){
            log.debug("No entries to send for global leaderboard id: {}",
                    globalLeaderboardEvent != null ? globalLeaderboardEvent.getId() : "null");
            return;
        }

        for(long i = 0; i < globalLeaderboardEvent.getTopNLeaderboard().size(); i++){

            simpMessagingTemplate.convertAndSend (
                    propertiesStorage.getGlobalPushEndpointPattern()
                            + globalLeaderboardEvent.getId(),

                    new LeaderboardResponsePayload (
                            globalLeaderboardEvent.getId(),
                            globalLeaderboardEvent.getTopNLeaderboard().get((int) i)
                                    .getUserId(),
                            globalLeaderboardEvent.getTopNLeaderboard().get((int) i)
                                    .getScore(),
                    i + 1
                    )
            );

        }
    }
}