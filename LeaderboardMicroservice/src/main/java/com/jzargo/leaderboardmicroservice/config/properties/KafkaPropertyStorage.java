package com.jzargo.leaderboardmicroservice.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@RefreshScope
@Validated
@Component
@ConfigurationProperties(prefix="kafka")
public class KafkaPropertyStorage {
    private final Topic topic = new Topic();

    @Getter
    public static class Topic{
        @NotNull
        @Min(1)
        private Integer inSyncReplicas;
        @NotNull
        @Min(1)
        private Integer replicas;
    }
}
