package com.englishapp.demo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class DemoPingDto {

    public record CreatePingRequest(
            @NotBlank @Size(max = 200) String message
    ) {}

    public record PingResponse(
            UUID id,
            String message,
            Instant createdAt
    ) {}
}
