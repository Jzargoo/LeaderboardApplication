package com.jzargo.websocketapi.service;

import com.jzargo.websocketapi.dto.InitUserScoreRequest;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final LeaderboardWebClient leaderboardWebClient;

    @Override
    public void initLeaderboardScore(String id) {
        try {

            leaderboardWebClient.initUserScore(new InitUserScoreRequest(id));

        } catch (FeignException.FeignServerException e) {
            log.error("Internal error in leaderboard microservice", e);

        } catch (FeignException.FeignClientException e) {
            log.error("Incorrect request to leaderboard microservice", e);
        }
    }
}
