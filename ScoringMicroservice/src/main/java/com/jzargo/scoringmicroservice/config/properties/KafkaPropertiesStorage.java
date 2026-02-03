package com.jzargo.scoringmicroservice.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@RefreshScope
@ConfigurationProperties("kafka")
public class KafkaPropertiesStorage {
    @NotNull
    private Topic topic;
    @NotNull
    private Headers headers;
    private Consumer consumer;

    @Data
    public static class Consumer{
        private String groupId;
    }

    @Data
    public static class Headers{
        @NotBlank
        @NotNull
        private String messageId;
        private String sagaId;
    }

    @Data
    public static class Topic{
        @NotNull
        private Names names;
        @Min(1)
        @NotNull
        private Byte insyncReplicas;
        @Min(1)
        @NotNull
        private Short replicas;
        @Min(1)
        @NotNull
        private Short partitions;

        @Data
        public static class Names{
            @NotNull
            @NotBlank
            private String commandStringScore;
            @NotNull
            @NotBlank
            private String sagaCreateLeaderboard;
            @NotNull
            @NotBlank
            private String leaderboardEvent;

            @NotNull
            @NotBlank
            private String debeziumScoring;
            @NotNull
            @NotBlank
            private String debeziumFailed;
            @NotNull
            @NotBlank
            private String debeziumDeleted;
        }
    }
}
