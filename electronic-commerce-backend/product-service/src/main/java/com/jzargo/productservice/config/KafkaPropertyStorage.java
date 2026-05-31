package com.jzargo.productservice.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class KafkaPropertyStorage {
    @NotNull
    private Topics topics;

    @Data
    public static class Topics{
        @NotNull
        private ProductEventsTopic productEventsTopic;

        @Data
        public static class ProductEventsTopic{
            @NotNull
            private String name;
            @NotNull
            private Integer numPartitions;
            @NotNull
            private Integer replicas;
            @NotNull
            private Integer inSyncReplicas;
        }
    }
}
