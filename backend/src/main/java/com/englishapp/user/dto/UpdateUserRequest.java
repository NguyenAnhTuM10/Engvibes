package com.englishapp.user.dto;

import com.englishapp.user.CEFRLevel;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 3, max = 50)
    private String username;

    private CEFRLevel cefrLevel;
}
