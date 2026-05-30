package com.englishapp.game.dto;

import java.util.List;
import java.util.UUID;

/**
 * Matching game: client nhận 2 danh sách xáo trộn riêng.
 * terms có cardId để client gửi lại khi chấm.
 * definitions chỉ là chuỗi — không tiết lộ mapping.
 */
public record MatchingGame(
        List<Term>   terms,
        List<String> definitions    // xáo trộn độc lập với terms
) {
    public record Term(UUID cardId, String front) {}
}
