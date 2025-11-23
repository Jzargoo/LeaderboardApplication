package com.jzargo.usermicroservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_added_created_leaderboard")
public class UserAddedCreatedLeaderboard {
    @Id
    private String id;
    private String sagaId;
    private long userId;
    private String lbId;
}
