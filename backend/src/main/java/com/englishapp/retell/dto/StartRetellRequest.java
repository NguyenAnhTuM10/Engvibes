package com.englishapp.retell.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartRetellRequest {
    @NotNull
    @Min(1) @Max(4)
    private Integer scaffoldLevel;
}
