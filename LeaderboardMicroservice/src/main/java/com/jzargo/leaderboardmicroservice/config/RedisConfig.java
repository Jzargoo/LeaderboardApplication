package com.jzargo.leaderboardmicroservice.config;

import com.jzargo.leaderboardmicroservice.handler.RedisGlobalLeaderboardUpdateHandler;
import com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;
import org.springframework.scripting.support.ResourceScriptSource;

import java.time.Duration;

import static org.springframework.data.redis.stream.StreamMessageListenerContainer.*;

@Configuration
@Slf4j
public class RedisConfig {

    public static final String GLOBAL_STREAM_KEY = "global-leaderboard-stream";
    public static final String GLOBAL_GROUP_NAME = "global-consumer-group";
    public static final String LOCAL_STREAM_KEY = "local-leaderboard-stream";
    public static final String LOCAL_GROUP_NAME = "local-consumer-group";

    @Bean
    RedisScript<String> mutableLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("/scripts/increase_user_score.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    RedisScript<String> sagaSuccessfulScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("/scripts/SagaSuccessfulStep.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }


    @Bean
    RedisScript<String> createLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("/scripts/create_leaderboard.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    RedisScript<String> createUserCachedScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("/scripts/create_user_cached.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    RedisScript<String> immutableLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("/scripts/update_user_score.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }


    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisLocalLeaderboardHandler redisLocalLeaderboardHandler,
            RedisGlobalLeaderboardUpdateHandler redisGlobalLeaderboardUpdateHandler
            ) {

        StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> options =
                StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(500))
                        .build();

        StreamMessageListenerContainer<String, MapRecord<String, String, String>> container
                = create(redisConnectionFactory, options);

        container.receiveAutoAck(
                Consumer.from(LOCAL_GROUP_NAME, "consumer-1"),
                StreamOffset.create(LOCAL_STREAM_KEY, ReadOffset.lastConsumed()),
                redisLocalLeaderboardHandler
        );

        container.receiveAutoAck(
                Consumer.from(GLOBAL_GROUP_NAME, "consumer-1"),
                StreamOffset.create(GLOBAL_STREAM_KEY, ReadOffset.lastConsumed()),
                redisGlobalLeaderboardUpdateHandler
        );

        container.start();
        return container;
    }
}