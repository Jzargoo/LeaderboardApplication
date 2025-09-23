package messaging;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class UserEventHappenedCommand {
    private String lbId;
    private String eventName;
    private Long userId;
    private Map<String, Object> metadata;
}
