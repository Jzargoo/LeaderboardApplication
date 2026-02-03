package com.jzargo.websocketapi.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@ConfigurationProperties(prefix="kafka")
public class KafkaPropertiesStorage {
    @NotNull
    private Topic topic;
    @NotNull
    private Consumer consumer;
    @NotNull
    private Headers headers;

    @Data
    public static class Consumer{
        @NotNull
        @NotBlank
        private String groupId;
    }

    @Data
    public static class Headers{
        private String sagaId;
        @NotNull
        @NotBlank
        private String messageId;
    }

    @Data
    public static class Topic{
        @NotNull
        @Min(1)
        private Short inSyncReplicas;

        @NotNull
        @Min(1)
        private Short replicas;

        @NotNull
        @Min(1)
        private Short partitions;

        @NotNull
        private Names names;

        @Data
        public static class Names{
            @NotNull
            @NotBlank
            private String commandStringScore;
            @NotNull
            @NotBlank
            private String leaderboardEvent;
        }
    }


}
