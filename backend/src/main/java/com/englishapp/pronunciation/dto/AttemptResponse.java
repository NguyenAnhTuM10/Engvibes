package com.englishapp.pronunciation.dto;

import com.englishapp.sm2.dto.SoundCardChange;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Kết quả đầy đủ trả về cho client (qua WS và REST)
public record AttemptResponse(
        UUID attemptId,
        int attemptNumber,
        String transcript,
        String targetIpa,
        String actualIpa,
        int overallScore,
        int accuracyScore,
        int fluencyScore,
        List<PhonemeMatch> phonemeMatches,
        List<WordAnalysis> wordAnalyses,  // per-word breakdown (null khi load từ history)
        // Thay đổi vừa áp lên deck "Sounds to practice" (rỗng nếu không có từ yếu).
        // Để FE báo "Đã thêm 'think' vào danh sách ôn". Null khi load từ history.
        List<SoundCardChange> soundCardChanges,
        Instant createdAt
) {}
