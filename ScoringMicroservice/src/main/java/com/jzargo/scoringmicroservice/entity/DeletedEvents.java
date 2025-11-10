package com.jzargo.scoringmicroservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "deleted_events")
public class DeletedEvents {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Integer id;
    private String lbId;
    private String sagaId;
}
