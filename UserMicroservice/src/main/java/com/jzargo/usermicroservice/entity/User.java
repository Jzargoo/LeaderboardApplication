package com.jzargo.usermicroservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
    @ElementCollection
    @CollectionTable(
            name = "active_leaderboards",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    private List<String> activeLeaderboards = new ArrayList<>();
}
