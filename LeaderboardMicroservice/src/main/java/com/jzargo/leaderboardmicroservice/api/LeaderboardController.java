package com.jzargo.leaderboardmicroservice.api;

import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PostMapping
    public ResponseEntity<String> createLeaderboard(
            @RequestBody @Validated CreateLeaderboardRequest request,
            @AuthenticationPrincipal Long userId) {
        try {
            leaderboardService.createLeaderboard(request, userId);
        } catch (Exception e) {
            log.error("creation leaderboard exit with exception: {}", String.valueOf(e));
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok("leaderboard created");
    }
}
