package com.englishapp.sm2.dto;

import java.util.List;

/** Kết quả sau khi import batch — luôn trả về kể cả khi có lỗi một phần. */
public record ImportSummary(
        int imported,
        int skipped,            // trùng front (dupe) trong deck
        List<ImportError> errors
) {
    public record ImportError(int line, String reason) {}
}
