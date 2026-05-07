package com.englishapp.session.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MarkWarmupRequest {
    @NotNull
    private UUID vocabId;
    @NotNull
    private String status; // "known" | "new"
}
