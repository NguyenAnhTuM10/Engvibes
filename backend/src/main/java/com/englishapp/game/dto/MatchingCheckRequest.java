package com.englishapp.game.dto;

import java.util.List;
import java.util.UUID;

public record MatchingCheckRequest(List<Pair> pairs) {
    public record Pair(UUID cardId, String matchedBack) {}
}
