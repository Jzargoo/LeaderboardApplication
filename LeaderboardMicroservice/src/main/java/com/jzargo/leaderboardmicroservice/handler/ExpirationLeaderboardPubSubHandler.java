package com.jzargo.leaderboardmicroservice.handler;

import com.jzargo.leaderboardmicroservice.entity.LeaderboardInfo;
import com.jzargo.leaderboardmicroservice.repository.LeaderboardInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class ExpirationLeaderboardPubSubHandler implements MessageListener {
    private final LeaderboardInfoRepository leaderboardInfoRepository;

    public ExpirationLeaderboardPubSubHandler(LeaderboardInfoRepository leaderboardInfoRepository) {
        this.leaderboardInfoRepository = leaderboardInfoRepository;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String expiredKey = message.toString();

        if(
                expiredKey.isBlank() &&
                !expiredKey.startsWith("leaderboard_signal")
        ) {
            log.debug("Message with uninteresting or blank key is received");
            return;
        }
        log.debug("leaderboard expiration key is caught");
        LeaderboardInfo byId = leaderboardInfoRepository.findById(expiredKey);
        if(expiredKey)
    }

}
