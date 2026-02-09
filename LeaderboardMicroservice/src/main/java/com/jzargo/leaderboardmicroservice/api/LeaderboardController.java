package com.jzargo.leaderboardmicroservice.api;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.dto.UserScoreResponse;
import com.jzargo.leaderboardmicroservice.config.properties.ApplicationPropertyStorage;
import com.jzargo.leaderboardmicroservice.dto.CreateLeaderboardRequest;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.exceptions.LeaderboardNotFound;
import com.jzargo.leaderboardmicroservice.exceptions.UserNotFoundInLeaderboard;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.leaderboardmicroservice.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final SagaLeaderboardCreate sagaLeaderboardCreate;
    private final ApplicationPropertyStorage applicationPropertyStorage;

    public LeaderboardController(LeaderboardService leaderboardService, SagaLeaderboardCreate sagaLeaderboardCreate, ApplicationPropertyStorage applicationPropertyStorage) {
        this.leaderboardService = leaderboardService;
        this.sagaLeaderboardCreate = sagaLeaderboardCreate;
        this.applicationPropertyStorage = applicationPropertyStorage;
    }

    @GetMapping("/score/{id}")
    public ResponseEntity<UserScoreResponse> getMyScoreIn(@PathVariable String id,
                                                          @RequestParam Long userId
                                                          ) {
        try {
            UserScoreResponse userScoreInLeaderboard = leaderboardService.getUserScoreInLeaderboard(
                    userId,
                    id
            );

            return ResponseEntity.ok(userScoreInLeaderboard);

        } catch (UserNotFoundInLeaderboard e) {

            return ResponseEntity.notFound().build();

        } catch (LeaderboardNotFound e) {

            return ResponseEntity.badRequest().build();

        }
    }

    @PostMapping("/create")
    public ResponseEntity<String> createLeaderboard(
            @RequestBody @Validated CreateLeaderboardRequest request,
            @RequestParam Long userId,
            @RequestParam String preferredUsername
            ) {

        try {

            if(
                    request.isMutable() && (
                            request.getEvents() ==null || request.getEvents().isEmpty()
                    )
            ) {

                return ResponseEntity.badRequest().body("Mutable leaderboard must contain at least 1 event");

            }

            sagaLeaderboardCreate.startSaga(request, userId,preferredUsername);

        } catch (Exception e) {

            log.error("creation leaderboard exit with exception: {}", String.valueOf(e));
            return ResponseEntity.badRequest().build();

        }
        log.info("leaderboard with name {} created by user with id {}",
                request.getName(), userId);
        return ResponseEntity.ok("leaderboard created");
    }

    @PutMapping
    public ResponseEntity<Void> initUserScore(
            @RequestBody @Validated InitUserScoreRequest request,
            @RequestParam long userId
    ) {


        if(leaderboardService.userExistsById(userId, request.getLeaderboardId())) {

            return ResponseEntity
                    .status(HttpStatusCode.valueOf(208))
                    .build();

        }
        try {

        leaderboardService.initUserScore(request,userId);
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

    @GetMapping("/view/{id}")
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

    @GetMapping("/view/participant/{lbId}")
    public ResponseEntity<Boolean> isParticipant(@PathVariable String lbId,
                                                 @RequestHeader("#{@applicationPropertyStorage.headers.userId}") Long userId) {
        return ResponseEntity.ok(
                leaderboardService.isParticipant(lbId, userId)
        );
    }
}