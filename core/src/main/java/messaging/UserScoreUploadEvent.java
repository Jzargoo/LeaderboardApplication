package messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class UserScoreUploadEvent {
    private String lbId;
    private Long userId;
    private double score;
    private String name;
    private Map<String, Object> metadata;
}