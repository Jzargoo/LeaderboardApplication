package com.jzargo.leaderboardmicroservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor @NoArgsConstructor
public class CreateLeaderboardRequest {
    @NotNull(message = "Description cannot be null")
    @Size(max = 255, min = 10,
            message = "Description cannot be longer than 255 characters or shorter than 10 characters")
    private String description;
    @Max(value = 100, message = "Global range cannot be greater than 100")
    @Min(value = 1, message = "Global range cannot be less than 1")
    private int globalRange;
    @NotNull(message = "Name cannot be null")
    @Size(max = 50, min = 3,
            message = "Name cannot be longer than 50 characters or shorter than 3 characters")
    private String name;
    private long ownerId;
    private int initialValue;
    private boolean isPublic;
    private boolean isMutable;
    private LocalDateTime expireAt;
    private double maxScore;
    private Set<String> regions;
    private int maxEventsPerUser;
    private int maxEventsPerUserPerDay;
    private boolean showTies;
    private Map<String, Double> events;
}
