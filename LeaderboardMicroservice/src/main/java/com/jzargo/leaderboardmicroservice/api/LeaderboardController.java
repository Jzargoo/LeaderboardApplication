package com.jzargo.leaderboardmicroservice.api;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.region.Regions;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.shaded.io.opentelemetry.proto.trace.v1.Status;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;

    public LeaderboardController(LeaderboardService leaderboardService, SagaLeaderboardCreate sagaLeaderboardCreate) {
        this.leaderboardService = leaderboardService;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
    }

    @PostMapping
    public ResponseEntity<String> createLeaderboard(
            @RequestBody @Validated CreateLeaderboardRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {

        try {

            if(
                    request.isMutable() && (
                            request.getEvents() ==null || request.getEvents().isEmpty()
                    )
            ) {

                return ResponseEntity.badRequest().body("Mutable leaderboard must contain at least 1 event");

            }

            String preferredUsername = jwt.getClaimAsString("preferred_username");
            long userId = Long.parseLong(jwt.getSubject());
            String region = jwt.getClaimAsString("region") == null?
                    Regions.GLOBAL.getCode():
                    Regions.fromStringCode(
                            jwt.getClaimAsString("region")).getCode();

            sagaLeaderboardCreate.startSaga(request, userId,preferredUsername,region);

        } catch (Exception e) {

            log.error("creation leaderboard exit with exception: {}", String.valueOf(e));
            return ResponseEntity.badRequest().build();

        }

        return ResponseEntity.ok("leaderboard created");
    }

    @PutMapping
    public ResponseEntity<Void> initUserScore(
            @RequestBody @Validated InitUserScoreRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        String preferredUsername = jwt.getClaimAsString("preferred_username");
        long userId = Long.parseLong(jwt.getSubject());
        String region = jwt.getClaimAsString("region") == null?
                Regions.GLOBAL.getCode():
                Regions.fromStringCode(
                        jwt.getClaimAsString("region")).getCode();

        if(leaderboardService.userExistsById(userId, request.getLeaderboardId())) {

            return ResponseEntity
                    .status(HttpStatusCode.valueOf(208))
                    .build();

        }
        try {

        leaderboardService.initUserScore(request, preferredUsername, userId, region);
        return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {

            log.error("level of private exception");
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {

            log.error("Error happened when user with id {} tried to join to lb with id {}",
                    userId, request.getLeaderboardId());
            return ResponseEntity.internalServerError().build();

        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaderboardResponse> getLeaderboard(@PathVariable String id) {

        try {

        LeaderboardResponse leaderboard = leaderboardService.getLeaderboard(id);
        return ResponseEntity.ok(leaderboard);

        } catch (Exception e) {

            log.error("Caught exception during the getting leaderboard");
            return ResponseEntity
                    .badRequest().build();

        }
    }
}