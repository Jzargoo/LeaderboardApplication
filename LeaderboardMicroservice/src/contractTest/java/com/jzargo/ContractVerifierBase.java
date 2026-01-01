package com.jzargo;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.api.LeaderboardController;
import com.jzargo.leaderboardmicroservice.client.FactoryLeaderboardWebProxy;
import com.jzargo.leaderboardmicroservice.client.ScoringKafkaWebProxy;
import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.entity.SagaControllingState;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import com.jzargo.leaderboardmicroservice.repository.SagaControllingStateRepository;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreateImpl;
import com.jzargo.leaderboardmicroservice.service.LeaderboardServiceImpl;
import com.jzargo.messaging.LeaderboardEventInitialization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.mockito.InjectMocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.verifier.messaging.boot.AutoConfigureMessageVerifier;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("contract-test")
@WebMvcTest(controllers = LeaderboardController.class)
@AutoConfigureMessageVerifier
@AutoConfigureMockMvc
public abstract class ContractVerifierBase {

    private static final Logger log =
            LoggerFactory.getLogger(ContractVerifierBase.class);

    @MockitoBean
    LeaderboardServiceImpl leaderboardService;

    @MockitoBean
    SagaControllingStateRepository sagaControllingStateRepository;

    @MockitoBean
    LeaderboardInfoRepository leaderboardInfoRepository;
    @MockitoBean
    LeaderboardServiceImpl leaderboardServiceImpl;

    @MockitoSpyBean
    SagaLeaderboardCreateImpl sagaLeaderboardCreate;

    @BeforeEach
    void setupMocks(TestInfo testInfo) {
        String name = testInfo.getDisplayName();
        log.info("Init mocks for contract: {}", name);

        if (name.contains("defaultMocks")) {
            doNothing().when(sagaLeaderboardCreate)
                    .startSaga(any(), anyLong(), anyString(), anyString());

            when(leaderboardService.userExistsById(anyLong(), anyString()))
                    .thenReturn(true);

            doNothing().when(leaderboardService)
                    .initUserScore(any(), anyLong());

            when(leaderboardService.getLeaderboard(anyString()))
                    .thenReturn(new LeaderboardResponse(
                            Map.of(1L, 100.0, 2L, 90.0, 3L, 80.0),
                            "Test Description",
                            "Test Leaderboard",
                            "lb123"
                    ));
        }
    }

    @BeforeEach
    void setForSagaLeaderboard(){
        doNothing()
                .when(leaderboardService)
                .deleteLeaderboard(
                        anyString(),
                        anyString()
                );

        when(leaderboardInfoRepository
                .findById(anyString())
        ).thenReturn(
                Optional.of(LeaderboardInfo.builder()
                        .ownerId(123L)
                        .build()
                )
        );

        when(sagaControllingStateRepository
                .findByLeaderboardId(anyString())
        ).thenReturn(List.of(
                new SagaControllingState()
        ));

        doNothing()
                .when(
                        sagaControllingStateRepository.save(
                                any(SagaControllingState.class)
                        )
                );
    }

    protected void initOutOfTimeEvent(){
        sagaLeaderboardCreate.stepOutOfTime("leaderboard455");
    }

    @BeforeEach
    void setupSecurity(TestInfo testInfo) {
        String name = testInfo.getDisplayName();

        if (name.contains("!user")) {
            SecurityContextHolder.clearContext();
            return;
        }

        if (name.contains("configUser?")) {
            String cfg = name.substring(name.indexOf("configUser?") + 11);
            String[] p = cfg.split("_");

            Jwt jwt = new Jwt(
                    "token",
                    null,
                    null,
                    Map.of("alg", "none"),
                    Map.of(
                            "preferred_username", p[0],
                            "user_id", p[1],
                            "region", p.length > 2 ? p[2] : "GLOBAL"
                    )
            );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(
                    new JwtAuthenticationToken(jwt, List.of())
            );

            SecurityContextHolder.setContext(context);
        }
    }
}
