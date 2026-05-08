package com.englishapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhonemeStatsResponse {
    private String phoneme;
    private int totalAttempts;
    private int errors;
    private double errorRate;
}
