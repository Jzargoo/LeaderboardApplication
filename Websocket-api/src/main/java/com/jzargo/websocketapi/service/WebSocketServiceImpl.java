package com.jzargo.websocketapi.service;

import com.jzargo.websocketapi.dto.InitUserScoreRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService{
    private final LeaderboardWebClient leaderboardWebClient;
    @Override
    public void initLeaderboardScore(String id) {
        leaderboardWebClient.initUserScore(new InitUserScoreRequest(id));
    }

}
