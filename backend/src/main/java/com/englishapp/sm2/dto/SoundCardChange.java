package com.englishapp.sm2.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Một thay đổi vừa xảy ra trên deck "Sounds to practice" sau 1 lần phát âm.
 * Trả về kèm AttemptResponse để frontend báo "Đã thêm 'think' vào danh sách ôn".
 *
 * @param action  ADDED   — từ yếu mới, vừa thêm card (due = now)
 *                DEMOTED — từ yếu đã có trong deck, phát âm lại vẫn sai → SM-2 hạ quality
 *                PROMOTED— từ đã có trong deck, lần này phát âm tốt → SM-2 review thành công
 */
public record SoundCardChange(
        UUID cardId,
        String word,
        String action,
        String ipa,
        int score,
        Integer intervalDays,
        Instant dueDate
) {}
