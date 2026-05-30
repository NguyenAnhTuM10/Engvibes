package com.englishapp.sm2.dto;

/** Import từ paste text kiểu Quizlet. */
public record ImportTextRequest(
        String content,
        /** Ký tự ngăn front|back trong 1 dòng (default TAB). */
        String termSep,
        /** Ký tự ngăn các card (default newline). */
        String cardSep
) {
    public ImportTextRequest {
        if (termSep == null || termSep.isEmpty()) termSep = "\t";
        if (cardSep == null || cardSep.isEmpty()) cardSep = "\n";
    }
}
