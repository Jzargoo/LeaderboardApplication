package com.jzargo.websocketapi;

import com.jzargo.websocketapi.service.LeaderboardWebClient;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@AutoConfigureStubRunner(
        ids = {"com.jzargo:leaderboard-microservice:+:stubs:8080"}
)
public class LeaderboardRestContractTest {

    @Value("${stubrunner.runningstubs.leaderboardmicroservice.port}")
    private int leaderboardServicePort;

    @Autowired
    private LeaderboardWebClient leaderboardWebClient;

    @DynamicPropertySource
    static void feignProperties(DynamicPropertyRegistry registry) {
        registry.add("leaderboard.url",
                () -> "http://localhost:" + System.getProperty("stubrunner.port", "8080")
        );
    }
}
