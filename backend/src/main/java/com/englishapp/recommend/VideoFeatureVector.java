package com.englishapp.recommend;

import com.englishapp.user.CEFRLevel;

import java.util.Map;
import java.util.UUID;

public record VideoFeatureVector(
        UUID videoId,
        CEFRLevel cefrLevel,
        String topic,
        Integer durationSec,
        Map<String, Double> phonemesDensity,
        double vocabDifficulty,
        double popularityScore
) {}
