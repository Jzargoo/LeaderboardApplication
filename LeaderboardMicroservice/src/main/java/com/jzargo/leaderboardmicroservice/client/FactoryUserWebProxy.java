package com.jzargo.leaderboardmicroservice.client;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class FactoryUserWebProxy {

    private final Map<String, UserServiceWebProxy> proxies;

    public UserServiceWebProxy getClient(String type) {
        if (type == null)
            return proxies.get(TypesOfProxy.KAFKA.name());

        return proxies.getOrDefault(
                type.toUpperCase(),
                proxies.get(TypesOfProxy.KAFKA.name())
        );
    }

    FactoryUserWebProxy(List<UserServiceWebProxy> userServiceWebProxyList){
        proxies = userServiceWebProxyList.stream()
                .collect(
                        Collectors.toMap(
                                proxy -> proxy.getType()
                                        .name().toLowerCase(),
                                proxy -> proxy
                        )
                );
    }
}
