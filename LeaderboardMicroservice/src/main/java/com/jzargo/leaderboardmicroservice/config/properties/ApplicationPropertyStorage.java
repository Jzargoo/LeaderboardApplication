package com.jzargo.leaderboardmicroservice.config.properties;

import com.jzargo.leaderboardmicroservice.client.TypesOfProxy;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@RefreshScope
@Validated
@Component
@ConfigurationProperties(prefix="application")
public class ApplicationPropertyStorage {

    @NotNull
    private Proxy proxy;
    @NotNull
    private Headers headers;
    @NotNull
    private Max max;

    @Data
    public static class Headers {
        @NotNull
        private String userId;
        @NotNull
        private String preferredUsername;
    }

    @Data
    public static class Max{
        @NotNull
        @Min(0)
        private Long durationForInactiveState;
    }

    @Data
    public static class Proxy{
        @NotNull
        private Mode mode;

        @Data
        public static class Mode{
            private TypesOfProxy scoring;
            private TypesOfProxy user;
            private TypesOfProxy leaderboard;
        }

    }

}
