package com.jzargo.leaderboardmicroservice.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FactoryScoringWebProxy {

    private final Map<String, ScoringServiceWebProxy> proxies;

    public ScoringServiceWebProxy getClient(String type) {
        return proxies.getOrDefault(type.toUpperCase(),
                proxies.get(TypesOfProxy.KAFKA.name())
        );
    }

    FactoryScoringWebProxy (List<ScoringServiceWebProxy> scoringServiceWebProxies){
        proxies = scoringServiceWebProxies.stream()
                .collect(
                        Collectors.toMap(
                                proxy -> proxy.getType()
                                        .name().toLowerCase(),
                                proxy -> proxy
                        )
                );
    }

}
