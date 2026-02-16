package com.jzargo.leaderboardmicroservice;

import com.jzargo.leaderboardmicroservice.config.properties.ApplicationPropertyStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationPropertyStorage.class)
public class LeaderboardMicroserviceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaderboardMicroserviceApplication.class, args);
    }

}
