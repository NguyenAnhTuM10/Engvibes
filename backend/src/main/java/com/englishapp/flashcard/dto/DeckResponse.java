package com.englishapp.flashcard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckResponse {
    private UUID id;
    private String name;
    private String color;
    private boolean isDefault;
    private long cardCount;
    private long dueCount;
}
