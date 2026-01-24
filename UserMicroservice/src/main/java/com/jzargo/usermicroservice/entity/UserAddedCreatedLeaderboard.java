package com.jzargo.usermicroservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_added_created_leaderboard")
public class UserAddedCreatedLeaderboard {
    @Id
    private String id;
    @OneToMany(mappedBy = "user_id")
    private List<User> users;
    private String lbId;
    private String sagaId;
}
