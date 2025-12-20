package com.jzargo.gatewayapi;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.gatewayapi.client.LeaderboardClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureStubRunner(
        ids = {"com.jzargo:joining-api:+:stubs:8080"},
        stubsMode = StubRunnerProperties.StubsMode.LOCAL)
public class JoiningApiLeaderboardPartIntegrationTest {

        @Autowired
        private LeaderboardClient leaderboardClient;

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
