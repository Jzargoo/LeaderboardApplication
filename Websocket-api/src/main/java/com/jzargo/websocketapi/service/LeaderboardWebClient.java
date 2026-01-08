package com.jzargo.websocketapi.service;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.dto.UserScoreResponse;
import com.jzargo.websocketapi.dto.InitUserScoreRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(
        name = "leaderboard-microservice",
        url = "/api/v1/leaderboard"
)
public interface LeaderboardWebClient {

    @PutMapping
    void initUserScore(@RequestBody InitUserScoreRequest initUserScoreRequest);

    @GetMapping("/view/{id}")
    LeaderboardResponse getLeaderboard(@PathVariable String id);

    @GetMapping("/view/participant/{lbId}")
    boolean isParticipant(@PathVariable String lbId, @RequestParam String userId);

    @GetMapping("/score/{lbId}")
    UserScoreResponse myScoreIn(@PathVariable String lbId, @RequestParam  Long userId);
}