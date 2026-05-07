package com.englishapp.session.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateSessionRequest {
    @NotNull
    private UUID videoId;
}
