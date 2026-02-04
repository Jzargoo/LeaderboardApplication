package com.jzargo.entities;

import jakarta.persistence.*;
import lombok.Setter;

@Entity
@Table(name = "incrementing_value")
public class IncrementedValue {

    public static final String USER_ID_COUNTER = "user_id_counter";
    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    private String name;
    @Column(name = "lastValue")
    private Long lastValue;

    public String getName () {
        return name;
    }

    public Long getId() {
        return id;
    }

    public Long incrementValue() {
        return ++lastValue;
    }
    public void initializeLastValue(long l) {
        lastValue = l;
    }
    public Long getLastValue(){
        return lastValue;
    }
}

