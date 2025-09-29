package messaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor @NoArgsConstructor
@Builder
public class UserLocalUpdateEvent {
    private String leaderboardId;
    @Builder.Default
    private List<UserLocalEntry> entries = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserLocalEntry {
        Long userId;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        Double score;
        Long rank;
    }
}
