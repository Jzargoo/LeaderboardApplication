package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Table(name = "leaderboards_events")
@Entity
@Data
@Builder
@AllArgsConstructor @NoArgsConstructor
public class LeaderboardEvents {
    @Id
    private String id;

    @ManyToMany(mappedBy = "leaderboards")
    @Builder.Default
    private List<LbEventType> events = new ArrayList<>();

    private boolean isPublic;
    private String metadata;

    public void addEvent(LbEventType event) {
        events.add(event);
        event.getLeaderboards().add(this);
    }
}
