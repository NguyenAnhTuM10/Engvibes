package com.englishapp.flashcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeckRequest {
    @NotBlank
    @Size(max = 100)
    private String name;

    private String color;
}
