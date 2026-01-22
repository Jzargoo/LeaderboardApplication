package com.jzargo.usermicroservice.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@RefreshScope
@ConfigurationProperties("application")
public class ApplicationPropertiesStorage {
    private Headers headers;
    private Images images;

    @Data
    public static class Headers {
        private KeycloakConnection keycloakConnection;

        @Data
        public static class KeycloakConnection {
            @NonNull
            @NotEmpty
            private String name;
            @NonNull
            @NotBlank
            private String value;
        }
    }

    @Data
    public static class Images {
        @NonNull
        private DirPath dirPath;
        @NonNull
        private Dump dump;

        @Data
        public static class Dump{
            @NonNull
            private String user;
        }

        @Data
        public static class DirPath{
            @NonNull
            private String user;
        }
    }
}