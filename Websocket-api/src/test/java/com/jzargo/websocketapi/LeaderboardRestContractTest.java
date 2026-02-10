package com.jzargo.websocketapi;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.websocketapi.service.LeaderboardWebClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@AutoConfigureStubRunner(
        ids = {"com.jzargo:leaderboard-microservice:+:stubs:8080"}
)
public class LeaderboardRestContractTest {

    @Value("${application:tests:blank:leaderboardId}:lb123")
    private String leaderboardId;

    @Value("${application:tests:blank:leaderboardName}")
    private String leaderboardName;

    @Value("${application:tests:blank:leaderboard}")
    private Map<Long, Double> leaderboardInfo;

    @Autowired
    private LeaderboardWebClient leaderboardWebClient;

    @DynamicPropertySource
    static void feignProperties(DynamicPropertyRegistry registry) {
        registry.add("leaderboard.url",
                () -> "http://localhost:" + System.getProperty("stubrunner.port", "8080")
        );
    }

    @Test
    public void testGetLeaderboard_successful(){
        LeaderboardResponse leaderboard =
                leaderboardWebClient.getLeaderboard(leaderboardId);

        assertNotNull(leaderboard,
                "the received leaderboard is null");

        assertEquals(leaderboard.getLeaderboardId(), leaderboardId,
                "there is a mismatch in id");
        assertEquals(leaderboard.getName(), leaderboardName,
                "there is a mismatch in name of leaderboard");

        assertEquals(leaderboard.getLeaderboard().size(), leaderboardInfo.size(),
                "Mismatch between a number of items");

        for(Map.Entry<Long, Double> leaderboardEntry: leaderboard.getLeaderboard().entrySet()) {
            assertEquals(leaderboardEntry.getValue(),
                    leaderboardInfo.getOrDefault(leaderboardEntry.getKey(), null)
            );
        }
    }

}
