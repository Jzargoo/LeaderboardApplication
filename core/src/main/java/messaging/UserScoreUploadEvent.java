package messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class UserScoreUploadEvent {
    private String lbId;
    private String userId;
    private int score;
    private String name;
    private Map<String, Object> metadata;
}