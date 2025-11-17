package com.jzargo.leaderboardmicroservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisLuaScriptsConfig {
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
    RedisScript<String> deleteLeaderboardScript(){
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("/scripts/delete_leaderboard_script.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

}
