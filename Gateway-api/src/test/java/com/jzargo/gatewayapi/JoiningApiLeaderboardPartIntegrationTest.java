package com.jzargo.gatewayapi;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.gatewayapi.client.LeaderboardClient;
import com.jzargo.gatewayapi.config.RoutesDestination;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureStubRunner(
        ids = {"com.jzargo:LeaderboardMicroservice:+:stubs"},
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
public class JoiningApiLeaderboardPartIntegrationTest {

    private LeaderboardClient leaderboardClient;

    @Value("${stubrunner.runningstubs.leaderboardmicroservice.port}")
    private int leaderboardServicePort;


    @BeforeEach
    public void setup() {
        RoutesDestination routesDestination = new RoutesDestination();
        routesDestination.setLeaderboardServiceUrl("http://localhost:" + leaderboardServicePort);

        leaderboardClient = new LeaderboardClient(
                WebClient.builder().build(),
                routesDestination
        );
    }

    @Test
    public void testGetLeaderboard() {
        LeaderboardResponse response = leaderboardClient.getLeaderboard("lb123").block();

        assertNotNull(response);
        assertEquals("lb123", response.getLeaderboardId());
        assertEquals("Test Leaderboard", response.getName());
        assertEquals("Test Description", response.getDescription());
        assertEquals(3, response.getLeaderboard().size());
        assertEquals(100, response.getLeaderboard().get(1L));

    }


}
