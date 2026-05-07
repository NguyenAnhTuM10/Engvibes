package com.englishapp.session.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SetScaffoldRequest {
    @NotNull
    @Min(1)
    @Max(4)
    private Integer level;
}
