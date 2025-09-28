package com.jzargo.leaderboardmicroservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalLeaderboardCache {
    private String leaderboardId;
    private List<Entry> payload;

    public static class Entry {
        private long userId;
        private double score;

        public Entry(double score) {
            this.score = score;
        }

        public long getUserId() {
            return userId;
        }

        public void setUserId(long userId) {
            this.userId = userId;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }
}
