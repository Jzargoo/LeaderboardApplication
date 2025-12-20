package com.jzargo.leaderboardmicroservice;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.api.LeaderboardController;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.leaderboardmicroservice.service.LeaderboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("contract-test")
@WebMvcTest(controllers = LeaderboardController.class)
@AutoConfigureMockMvc
public abstract class ContractVerifierBase {

    private static final Logger log =
            LoggerFactory.getLogger(ContractVerifierBase.class);

    @MockitoBean
    LeaderboardServiceImpl leaderboardService;

    @MockitoBean
    SagaLeaderboardCreate sagaLeaderboardCreate;

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
