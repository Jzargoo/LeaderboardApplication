package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "scoring_event")
public class ScoringEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventName;
    private Double score;

    @ManyToMany(
            targetEntity = LeaderboardEvents.class,
            fetch = FetchType.LAZY,
            cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "leaderboards_scoring_mapping",
            joinColumns = @JoinColumn(name = "scoring_event_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "leaderboard_events_id", referencedColumnName = "id")
    )
    @Builder.Default
    private List<LeaderboardEvents> leaderboards = new ArrayList<>();
}
