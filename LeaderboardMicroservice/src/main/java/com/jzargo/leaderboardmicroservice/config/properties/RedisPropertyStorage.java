package com.jzargo.leaderboardmicroservice.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@RefreshScope
@Component
@ConfigurationProperties(prefix="spring.data.redis")
public class RedisPropertyStorage {
    private String password;
    private Integer port;
    private String host;
    private Cluster cluster;

    @Data
    static public class Cluster{
        private List<String> clusterNodes;
    }
}
