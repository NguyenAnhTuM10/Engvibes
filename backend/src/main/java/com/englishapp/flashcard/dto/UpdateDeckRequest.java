package com.englishapp.flashcard.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeckRequest {
    @Size(max = 100)
    private String name;

    private String color;
}
