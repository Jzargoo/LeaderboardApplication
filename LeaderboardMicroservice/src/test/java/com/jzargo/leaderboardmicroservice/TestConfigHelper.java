package com.jzargo.leaderboardmicroservice;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@TestConfiguration
public class TestConfigHelper {
    @Bean
    public IntegrationTestHelper integrationTestHelper(StringRedisTemplate stringRedisTemplate,
                                                       RedisScript<String>createLeaderboardScript) {
        return new IntegrationTestHelper(stringRedisTemplate, createLeaderboardScript);
    }

    @Bean
    public TestConsumer testConsumer() {
        return new TestConsumer();
    }

    @Bean
    public SagaTestConsumer sagaTestConsumer (){
        return new SagaTestConsumer();
    }
}
