package com.jzargo.websocketapi.lifecylce;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "application")
@Data
public class PropertiesStorage {

    private Attribute attribute = new Attribute();
    private Query query = new Query();
    private Headers headers = new Headers();
    private EndpointsPattern endpointsPattern = new EndpointsPattern();

    @Data
    public static class Attribute {
        private String userId;
        private String leaderboardId;
    }

    @Data
    public static class Query {
        private String leaderboardId;
    }

    @Data
    public static class Headers {
        private String userId;
    }

    @Data
    public static class EndpointsPattern {
        private String globalLeaderboardPush;
        private String localLeaderboardPush;
    }
}
