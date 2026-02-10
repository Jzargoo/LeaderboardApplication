package com.jzargo.websocketapi.mapper;

import com.jzargo.dto.LeaderboardResponse;
import com.jzargo.dto.UserScoreResponse;
import com.jzargo.mapper.Mapper;
import com.jzargo.websocketapi.dto.LeaderboardRefreshResponse;
import org.springframework.stereotype.Component;

@Component
public class CreateLeaderboardRefreshRequestMapper implements Mapper<CreateLeaderboardRefreshRequestMapper.RefreshInfo, LeaderboardRefreshResponse> {

    @Override
    public LeaderboardRefreshResponse map(RefreshInfo from) {
        return LeaderboardRefreshResponse.builder()
                .leaderboardId(from.leaderboardResponse.getLeaderboardId())
                .rank(from.userScoreResponse().getRank())
                .score(from.userScoreResponse().getScore())
                .name(from.leaderboardResponse().getName())
                .description(from.leaderboardResponse.getDescription())
                .globalRange(from.leaderboardResponse().getLeaderboard())
                .build();
    }

    public record RefreshInfo(UserScoreResponse userScoreResponse, LeaderboardResponse leaderboardResponse) {}
}
