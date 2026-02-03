package com.jzargo.usermicroservice.entity;

import com.jzargo.messaging.OutOfTimeEvent;
import com.jzargo.messaging.UserNewLeaderboardCreated;
import com.jzargo.region.Regions;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Table(name="users")
@Entity
@Data
@AllArgsConstructor @NoArgsConstructor
@Builder
public class User {
    @Id
    private Long id;

    private String avatar;
    private String name;
    private String email;
    @Builder.Default
    private String region = Regions.GLOBAL.getCode();

    @ElementCollection
    @MapKeyColumn(
            name = "id"
    )
    @Column(name = "leaderboard_name")
    @CollectionTable(
            name = "created_leaderboards",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Map<String, String> createdLeaderboards = new HashMap<>();


    @ElementCollection
    @MapKeyColumn(
            name = "id"
    )
    @Column(name = "leaderboard_name")
    @CollectionTable(
            name = "active_leaderboards",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<String> activeLeaderboards = new HashSet<>();


    public void addActiveLeaderboard(String leaderboardName) {
        activeLeaderboards.add(leaderboardName);
    }

    public void removeActiveLeaderboard(String leaderboardName) {
        activeLeaderboards.remove(leaderboardName);
    }

    public void addCreatedLeaderboard(UserNewLeaderboardCreated userNewLeaderboardCreated) {
        if(
                userNewLeaderboardCreated.getName() == null || userNewLeaderboardCreated.getName().isBlank() ||
                userNewLeaderboardCreated.getLbId() ==null || userNewLeaderboardCreated.getLbId().isBlank() ) {
            return;
        }
        createdLeaderboards.put(userNewLeaderboardCreated.getLbId(), userNewLeaderboardCreated.getName());
    }

    public void removeCreatedLeaderboard(OutOfTimeEvent event) {
      if (event.getLeaderboardId() ==null || event.getLeaderboardId().isBlank()) {
          return;
      }

      createdLeaderboards.remove(event.getLeaderboardId());
    }
}
