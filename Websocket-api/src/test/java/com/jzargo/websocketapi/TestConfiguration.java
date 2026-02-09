package com.jzargo.websocketapi;

import com.jzargo.websocketapi.service.LeaderboardWebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestConfiguration {

    @Value("@{leaderboard.url}")
    private String lbUrl;

    @Bean
    LeaderboardWebClient feignClientBuilder(FeignClientBuilder feignClientBuilder){
        return feignClientBuilder
                .forType(LeaderboardWebClient.class, "leaderboard-microservice")
                .url(lbUrl)
                .build();
    }
}

