package com.englishapp.shadow;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPhonemeStatsId implements Serializable {
    private UUID userId;
    private String phoneme;
}
