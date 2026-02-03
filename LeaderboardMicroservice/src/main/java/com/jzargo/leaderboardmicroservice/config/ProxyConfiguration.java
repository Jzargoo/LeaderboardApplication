package com.jzargo.leaderboardmicroservice.config;

import com.jzargo.leaderboardmicroservice.client.*;
import com.jzargo.leaderboardmicroservice.config.properties.ApplicationPropertyStorage;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProxyConfiguration {

    @Bean
    @RefreshScope
    public LeaderboardServiceWebProxy leaderboardServiceWebProxy(
            ApplicationPropertyStorage applicationPropertyStorage,
            FactoryLeaderboardWebProxy factoryLeaderboardWebProxy
            ){
        return factoryLeaderboardWebProxy.getClient(
                applicationPropertyStorage.getProxy().getMode().getLeaderboard()
        );
    }

    @Bean
    @RefreshScope
    public UserServiceWebProxy userServiceWebProxy(
            ApplicationPropertyStorage applicationPropertyStorage,
            FactoryUserWebProxy factoryUserWebProxy
    ){
        return factoryUserWebProxy.getClient(
                applicationPropertyStorage.getProxy().getMode().getLeaderboard()
        );
    }

    @Bean
    @RefreshScope
    public ScoringServiceWebProxy scoringServiceWebProxy(
            ApplicationPropertyStorage applicationPropertyStorage,
            FactoryScoringWebProxy factoryScoringWebProxy
    ){
        return factoryScoringWebProxy.getClient(
                applicationPropertyStorage.getProxy().getMode().getLeaderboard()
        );
    }
}
