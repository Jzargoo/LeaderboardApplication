package com.jzargo.messaging;

import com.jzargo.region.Regions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor @NoArgsConstructor
@Data
public class UserUpdateEvent {
    private String name;
    private Long id;
    private Regions region;
    private String email;
}
