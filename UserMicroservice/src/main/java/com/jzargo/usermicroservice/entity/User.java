package com.jzargo.usermicroservice.entity;

import com.jzargo.region.Regions;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
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
    @CollectionTable(
            name = "active_leaderboards",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private Set<String> activeLeaderboards = new HashSet<>();

    public void addActiveLeaderboard(String leaderboardName) {
        activeLeaderboards.add(leaderboardName);
    }
}
