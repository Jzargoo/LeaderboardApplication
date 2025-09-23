package messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor @Data
public class UserScoreEvent {
    private double score;
    private String lbId;
    private Long userId;
    private Map<String, Object> metadata;
}
