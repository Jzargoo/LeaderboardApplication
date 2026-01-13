package com.jzargo.leaderboardmicroservice.config.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@RefreshScope
@Validated
@Component
@ConfigurationProperties(prefix="spring.data.redis")
public class RedisPropertyStorage {
    @NotNull
    @NotEmpty
    private String password;
    private Integer port;
    private String host;
    private Cluster cluster;

    @Data
    static public class Cluster{
        private List<String> clusterNodes;
    }
}
