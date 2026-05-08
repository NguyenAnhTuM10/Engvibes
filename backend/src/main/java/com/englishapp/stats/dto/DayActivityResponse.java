package com.englishapp.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayActivityResponse {
    private LocalDate date;
    private int totalMinutes;
    private Map<String, Integer> byActivity;
}
