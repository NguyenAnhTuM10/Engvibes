package com.englishapp.user.dto;

import com.englishapp.user.CEFRLevel;
import com.englishapp.user.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String username;
    private CEFRLevel cefrLevel;
    private Role role;
    private int totalXp;
    private int currentStreakDays;
    private Instant createdAt;
}
