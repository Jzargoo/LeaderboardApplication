package com.jzargo.leaderboardmicroservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {
    @Bean
    LettuceConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory();
    }

    @Bean
    RedisScript<String> mutableLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(
                new ClassPathResource("/scripts/increase_user_score.lua").toString()
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    RedisScript<String> createLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(
                new ClassPathResource("/scripts/create_leaderboard.lua").toString()
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    RedisScript<String> createUserCachedScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(
                new ClassPathResource("/scripts/create_user_cached.lua").toString()
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    RedisScript<String> immutableLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(
                new ClassPathResource("/scripts/update_user_score.lua").toString()
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

}
