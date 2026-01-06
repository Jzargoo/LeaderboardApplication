package com.jzargo.websocketapi.lifecylce;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class PropertiesStorage {
    @Value("application.attribute.userId:user_id")
    private String userIdAttribute;
    @Value("application.attribute.leaderboardId:leaderboard_id")
    private String leaderboardAttribute;

    @Value("application.query.leaderboardId:leaderboardId")
    private String leaderboardQuery;
    @Value("application.headers.userId:X-USER-ID")
    private String userIdHeader;

    @Value("application.endpoints-pattern.global-leaderboard-push:/topic/leaderboard-update/")
    private String globalPushEndpointPattern;
    @Value("application.endpoints-pattern.local-leaderboard-push:/queue/leaderboard-update/")
    private String localPushEndpointPattern;


}
