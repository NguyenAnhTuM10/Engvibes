package com.englishapp.recommend;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVideoInteractionId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "video_id")
    private UUID videoId;
}
