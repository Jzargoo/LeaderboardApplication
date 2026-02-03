package com.jzargo.usermicroservice.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor @NoArgsConstructor
public class UserResponse {
    private long userId;
    private String name;
    private String regionCode;
    private byte[] avatar;
    private Set<String> activeLeaderboards;
}
