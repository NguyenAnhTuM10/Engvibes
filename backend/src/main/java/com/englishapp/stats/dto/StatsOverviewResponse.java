package com.englishapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsOverviewResponse {
    private int streakDays;
    private int totalXp;
    private long videosCompleted;
    private long vocabMastered;
    private double avgRetellScore7d;
}
