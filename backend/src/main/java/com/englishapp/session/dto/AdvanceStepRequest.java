package com.englishapp.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdvanceStepRequest {
    @NotNull
    @Min(0)
    @Max(6)
    private Integer step;

    @NotNull
    private String action; // "complete" | "skip"
}
