package com.englishapp.stats;

import com.englishapp.common.ApiResponse;
import com.englishapp.stats.dto.DayActivityResponse;
import com.englishapp.stats.dto.PhonemeStatsResponse;
import com.englishapp.stats.dto.StatsOverviewResponse;
import com.englishapp.user.CEFRLevel;
import com.englishapp.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Analytics", description = "Learning stats and progress tracking")
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final UserService userService;

    @GetMapping("/overview")
    public ApiResponse<StatsOverviewResponse> overview() {
        return ApiResponse.ok(statsService.getOverview(currentUserId()));
    }

    @GetMapping("/weekly")
    public ApiResponse<List<DayActivityResponse>> weekly() {
        return ApiResponse.ok(statsService.getWeeklyActivity(currentUserId()));
    }

    @GetMapping("/phonemes")
    public ApiResponse<List<PhonemeStatsResponse>> phonemes() {
        return ApiResponse.ok(statsService.getPhonemeStats(currentUserId()));
    }

    @GetMapping("/vocab-growth")
    public ApiResponse<Map<LocalDate, Map<CEFRLevel, Integer>>> vocabGrowth() {
        return ApiResponse.ok(statsService.getVocabGrowth(currentUserId()));
    }

    private UUID currentUserId() {
        return userService.getCurrentUser().getId();
    }
}
