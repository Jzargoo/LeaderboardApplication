package com.jzargo.usermicroservice.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@RefreshScope
@ConfigurationProperties("application")
@Validated
public class ApplicationPropertiesStorage {
    private Headers headers;

    @Data
    public static class Headers{
        private KeycloakConnection keycloakConnection;
        @Data
        public static class KeycloakConnection{
            private String name;
            private String value;
        }
    }
}
