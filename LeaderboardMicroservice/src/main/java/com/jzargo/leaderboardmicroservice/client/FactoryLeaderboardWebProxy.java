package com.jzargo.leaderboardmicroservice.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FactoryLeaderboardWebProxy {

    private final Map<String, LeaderboardServiceWebProxy> proxies;

    public LeaderboardServiceWebProxy getClient(String type) {
        if (type == null)
            return proxies.get(TypesOfProxy.KAFKA.name());

        return proxies.getOrDefault(type.toUpperCase(),
                proxies.get(TypesOfProxy.KAFKA.name())
        );
    }

    FactoryLeaderboardWebProxy (List<LeaderboardServiceWebProxy> leaderboardServiceWebProxies){
        proxies = leaderboardServiceWebProxies.stream()
                .collect(
                        Collectors.toMap(
                                proxy -> proxy.getType()
                                        .name().toLowerCase(),
                                proxy -> proxy
                        )
                );
    }

}
