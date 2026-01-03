package com.jzargo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "incrementing_value")
public class IncrementedValue {

    public static final String USER_ID_COUNTER = "user_id_counter";
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    @Column(name = "lastValue")
    private Long lastValue;

    public void setName(String name) {
        this.name = name;
    }
    public String getName () {
        return name;
    }
    public void setId(Long id) {
        this.id = id;
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

