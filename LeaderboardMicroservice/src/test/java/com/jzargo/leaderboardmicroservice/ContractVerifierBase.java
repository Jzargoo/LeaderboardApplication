package com.jzargo.leaderboardmicroservice;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.leaderboardmicroservice.dto.InitUserScoreRequest;
import com.jzargo.leaderboardmicroservice.saga.SagaLeaderboardCreate;
import com.jzargo.leaderboardmicroservice.service.LeaderboardServiceImpl;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("contract-test")
public class ContractVerifierBase {

    private static final Logger log = LoggerFactory.getLogger(ContractVerifierBase.class);

    @MockitoBean
    private LeaderboardServiceImpl leaderboardService;

    @MockitoBean
    private SagaLeaderboardCreate sagaLeaderboardCreate;

    @BeforeEach
    public void mockServices(TestInfo testInfo) {
        String displayName = testInfo.getDisplayName();

        log.info("Contract test configuration mock services for test: {} started initializing", displayName);

        if (displayName.contains("defaultMocks")) {


            doNothing().when(sagaLeaderboardCreate).startSaga(any(), anyLong(), anyString(), anyString());
            when(leaderboardService.userExistsById(anyLong(), anyString())).thenReturn(true);
            doNothing().when(leaderboardService)
                    .initUserScore(any(InitUserScoreRequest.class), anyLong());
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
    public void initSecurity(TestInfo testInfo) {
        String displayName = testInfo.getDisplayName();

        log.info("Contract test configuration init security context for test: {} started initializing", displayName);
        Jwt jwt;
        if(displayName.contains("!user")) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(null);
            SecurityContextHolder.setContext(context);
        }
        else if ("configUser?".contains(displayName)){

            int i = displayName.indexOf("configUser?");

            String configurationName = displayName.substring(i + "configUser?".length());
            String[] parts = configurationName.split("_");
            String username = parts.length > 0 ? parts[0] : "defaultUser";
            String userId = parts.length > 1 ? parts[1] : "123";
            String region = parts.length > 2 ? parts[2] : "GLOBAL";

            jwt = new Jwt(
                    "tokenValue",
                    null,
                    null,
                    Map.of("alg", "none"),
                    Map.of(
                            "preferred_username", username,
                            "user_id", userId,
                            "region", region
                    )
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new Authentication() {
                @Override public Object getPrincipal() { return jwt; }
                @Override public boolean isAuthenticated() { return true; }
                @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
                @Override public Object getCredentials() { return null; }
                @Override public Object getDetails() { return null; }
                @Override public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}
                @Override public String getName() { return username; }
            });
            SecurityContextHolder.setContext(context);
        }

    }

    @Bean
    public Jwt mockJwt() {
        return new Jwt(
                "tokenValue",
                null,
                null,
                Map.of("alg", "none"),
                Map.of(
                        "preferred_username", "testuser",
                        "user_id", "123",
                        "region", "GLOBAL"
                )
        );
    }
    @Bean
    public void setupSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new Authentication() {
            @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
            @Override public Object getCredentials() { return null; }
            @Override public Object getDetails() { return null; }
            @Override public Object getPrincipal() { return mockJwt(); }
            @Override public boolean isAuthenticated() { return true; }
            @Override public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}
            @Override public String getName() { return "testuser"; }
        });
        SecurityContextHolder.setContext(context);
    }
}