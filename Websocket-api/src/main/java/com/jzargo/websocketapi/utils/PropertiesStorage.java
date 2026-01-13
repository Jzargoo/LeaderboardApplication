package com.jzargo.websocketapi.utils;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@RefreshScope
@ConfigurationProperties(prefix = "application")
public class PropertiesStorage {

    private Attribute attribute;
    private Query query;
    private Headers headers;
    private EndpointsPattern endpointsPattern;

    @Data
    public static class Attribute {
        private String userId;
        private String leaderboardId;
    }

    @Data
    public static class Query {
        private String leaderboardId;
    }

    @Data
    public static class Headers {
        @NotNull
        @NotBlank
        private String userId;
    }

    @Data
    public static class EndpointsPattern {
        @NotNull
        private String globalLeaderboardPush;
        @NotNull
        private String localLeaderboardPush;
    }
}
