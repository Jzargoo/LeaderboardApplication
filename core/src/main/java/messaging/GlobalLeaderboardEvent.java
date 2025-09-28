package messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GlobalLeaderboardEvent {
    private String id;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    @Builder.Default
    private List<Entry> topNLeaderboard = new ArrayList<>(10);

    @Data
    @AllArgsConstructor @NoArgsConstructor
    public static class Entry {
        private long userId;
        private double score;
    }
}
