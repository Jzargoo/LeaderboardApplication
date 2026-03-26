package com.jzargo.productservice.entity;

import jakarta.persistence.*;
import lombok.Data;


@Data
@Table
@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

}
