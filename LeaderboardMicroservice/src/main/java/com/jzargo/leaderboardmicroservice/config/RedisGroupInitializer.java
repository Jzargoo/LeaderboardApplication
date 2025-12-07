package com.jzargo.leaderboardmicroservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;

import static com.jzargo.leaderboardmicroservice.config.RedisConfig.*;

@Slf4j
public class RedisGroupInitializer {
    private final StringRedisTemplate stringRedisTemplate;

    public RedisGroupInitializer(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init(){
        createGroupIfNotExists(GLOBAL_STREAM_KEY, GLOBAL_GROUP_NAME);
        createGroupIfNotExists(LOCAL_STREAM_KEY, LOCAL_GROUP_NAME);

    }

    private void createGroupIfNotExists(String streamKey, String groupName) {
        try {
            stringRedisTemplate.execute((RedisCallback<Boolean>) connection -> {
                try {
                    connection.streamCommands()
                            .xGroupCreate(
                                    streamKey.getBytes(),
                                    groupName,
                                    ReadOffset.from("0-0"),
                                    true
                            );
                } catch (Exception e) {
                    if(e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                        log.debug("Consumer group {} for stream {} already exists", groupName, streamKey);
                    } else {
                        log.error("Could not create consumer group {} for stream {}", groupName, streamKey, e);
                    }
                }
                return true;
            });
            log.info("Created consumer group {} for stream {}", groupName, streamKey);
        } catch (Exception e) {
            if(e.getMessage() != null && e.getMessage().contains("BUSYGROUP")){
                log.debug("Consumer group {} for stream {} already exists", groupName, streamKey);
            } else {
                log.error("Could not create consumer group {} for stream {}", groupName, streamKey, e);
            }
        }
    }
}