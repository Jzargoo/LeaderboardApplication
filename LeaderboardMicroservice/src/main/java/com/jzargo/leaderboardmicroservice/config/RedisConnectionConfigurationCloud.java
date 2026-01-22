package com.jzargo.leaderboardmicroservice.config;

import com.jzargo.leaderboardmicroservice.config.properties.RedisPropertyStorage;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;


@Configuration
@EnableConfigurationProperties(RedisPropertyStorage.class)
@Profile("cloudconfig")
@ConditionalOnBooleanProperty(name = "redis.enabled", matchIfMissing = true)
public class RedisConnectionConfigurationCloud {


    private final RedisPropertyStorage redisPropertyStorage;

    public RedisConnectionConfigurationCloud(RedisPropertyStorage redisPropertyStorage) {
        this.redisPropertyStorage = redisPropertyStorage;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(
            redisPropertyStorage.getCluster().getClusterNodes()
        );
        clusterConfig.setPassword(RedisPassword.of(redisPropertyStorage.getPassword()));

        ClientOptions clientOptions = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientOptions(clientOptions)
                .build();

        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
