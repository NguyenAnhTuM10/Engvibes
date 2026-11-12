package com.englishapp.flashcard.dto;

import com.englishapp.flashcard.CardSource;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Tạo card. Hai cách:
 * <ul>
 *   <li>Card từ điển: gửi {@code vocabId}.</li>
 *   <li>Card tự tạo: gửi {@code front} + {@code back} (và {@code ipa} nếu có), bỏ trống {@code vocabId}.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCardRequest {
    private UUID vocabId;

    @Size(max = 500)
    private String front;

    @Size(max = 2000)
    private String back;

    @Size(max = 255)
    private String ipa;

    private UUID deckId;
    private String contextSentence;
    private UUID sourceVideoId;
    private UUID sourceSegmentId;
    private CardSource sourceType;

    @JsonIgnore
    @AssertTrue(message = "Provide either vocabId or both front and back")
    public boolean isContentValid() {
        if (vocabId != null) return true;
        return front != null && !front.isBlank() && back != null && !back.isBlank();
    }
}
