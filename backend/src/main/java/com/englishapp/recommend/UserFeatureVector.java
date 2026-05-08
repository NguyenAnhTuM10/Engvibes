package com.englishapp.recommend;

import com.englishapp.user.CEFRLevel;

import java.util.List;
import java.util.Map;

public record UserFeatureVector(
        CEFRLevel cefrLevel,
        List<String> weakPhonemes,
        List<String> recentTopics,
        Map<CEFRLevel, Integer> vocabKnownByCefr,
        int activeSessionCount
) {}
