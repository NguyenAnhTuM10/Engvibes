package com.englishapp.recommend.dto;

import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.video.dto.VideoResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyChallengeResponse {
    private VideoResponse recommendedVideo;
    private List<CardResponse> vocabsToReview;
    private String randomPhrase;
}
