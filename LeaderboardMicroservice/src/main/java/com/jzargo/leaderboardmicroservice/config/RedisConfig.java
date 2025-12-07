package com.jzargo.leaderboardmicroservice.config;

import com.jzargo.leaderboardmicroservice.handler.ExpirationLeaderboardPubSubHandler;
import com.jzargo.leaderboardmicroservice.handler.RedisGlobalLeaderboardUpdateHandler;
import com.jzargo.leaderboardmicroservice.handler.RedisLocalLeaderboardHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

import java.time.Duration;

import static org.springframework.data.redis.stream.StreamMessageListenerContainer.*;

@Configuration
@Profile("!standalone")
@Import(RedisLuaScriptsConfig.class)
@Slf4j
public class RedisConfig {

    public static final String GLOBAL_STREAM_KEY = "global-leaderboard-stream";
    public static final String GLOBAL_GROUP_NAME = "global-consumer-group";
    public static final String LOCAL_STREAM_KEY = "local-leaderboard-stream";
    public static final String LOCAL_GROUP_NAME = "local-consumer-group";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            MessageListenerAdapter messageListenerAdapter
            ){

        redisConnectionFactory.getConnection()
                .serverCommands().setConfig("notify-keyspace-events", "Ex");

        RedisMessageListenerContainer redisMessageListenerContainer
                = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);

        for(short db = 0; db < 16; db++) {
            redisMessageListenerContainer.addMessageListener(
                    messageListenerAdapter,
                    new PatternTopic(
                            "__keyevent@" + db + "__:expired")
            );
        }

        return redisMessageListenerContainer;
    }

    @Bean
    public MessageListenerAdapter messageListenerAdapter(ExpirationLeaderboardPubSubHandler subscriber){
        return new MessageListenerAdapter(subscriber, "onMessage");
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

    @Bean
    public RedisGroupInitializer redisGroupInitializer(StringRedisTemplate stringRedisTemplate){
        return new RedisGroupInitializer(stringRedisTemplate);
    }
}
