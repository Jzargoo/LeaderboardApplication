package com.jzargo.productservice.config;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Data
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "application")
public class ApplicationPropertyStorage {
    @NotNull
    private Image image;

    @Data
    public static class Image {
        @NotNull
        private String path;
    }
}
