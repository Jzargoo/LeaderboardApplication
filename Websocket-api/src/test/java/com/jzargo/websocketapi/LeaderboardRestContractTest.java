package com.jzargo.websocketapi;

import org.junit.runner.RunWith;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@AutoConfigureStubRunner(
        ids = {"com.jzargo:leaderboard-microservice:+:stubs:8080"}
)
public class LeaderboardRestContractTest {

}
