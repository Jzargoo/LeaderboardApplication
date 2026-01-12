package com.jzargo.leaderboardmicroservice.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@RefreshScope
@Validated
@Component
@Target({ElementType.TYPE})
@ConfigurationProperties(prefix = "kafka")
public @interface PropertiesStorage {
}
