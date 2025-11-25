package com.jzargo.websocketapi.service;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.websocketapi.dto.InitUserScoreRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(
        name = "leaderboard-microservice",
        url = "/api/v1"
)
public interface LeaderboardWebClient {

    @PutMapping
    void initUserScore(@RequestBody InitUserScoreRequest initUserScoreRequest);

    @GetMapping("/{id}")
    LeaderboardResponse getLeaderboard(@PathVariable String id);
}