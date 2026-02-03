package com.jzargo.leaderboardmicroservice.config;

import com.jzargo.leaderboardmicroservice.config.properties.RedisPropertyStorage;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableConfigurationProperties(RedisPropertyStorage.class)
@ConditionalOnBooleanProperty(name = "redis.enabled", matchIfMissing = true)
@Profile("devconfig")
public class RedisConnectionConfigurationDev{
    private final RedisPropertyStorage redisPropertyStorage;

    public RedisConnectionConfigurationDev(RedisPropertyStorage redisPropertyStorage) {
        this.redisPropertyStorage = redisPropertyStorage;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisPropertyStorage.getHost(), redisPropertyStorage.getPort());

        config.setPassword(RedisPassword.of(redisPropertyStorage.getPassword()));

        ClientOptions clientOptions = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .build();
        return new LettuceConnectionFactory(config, clientConfig);
    }

}
