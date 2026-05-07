package com.englishapp.ai.dto;

import java.util.List;
import java.util.Map;

public record VideoEnrichment(
        List<WarmupWord> warmupWords,
        Map<String, List<String>> collocations
) {}
