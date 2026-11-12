package com.englishapp.flashcard;

import com.englishapp.vocab.VocabEntry;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Một thẻ ôn tập (FSRS) của user. Có 2 loại:
 * <ul>
 *   <li>Card từ điển — {@code vocab != null}, nội dung lấy từ {@link VocabEntry}.</li>
 *   <li>Card tự tạo — {@code vocab == null}, nội dung nằm ở {@code front/back/ipa}
 *       (import text/CSV/JSON, thêm từ transcript video).</li>
 * </ul>
 * Dùng {@link #resolvedFront()}, {@link #resolvedBack()}, {@link #resolvedIpa()} để đọc
 * nội dung mà không cần phân biệt loại. Gọi trong transaction vì {@code vocab} là LAZY.
 */
@Entity
@Table(name = "user_cards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    /** Null với card tự tạo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocab_id")
    private VocabEntry vocab;

    /** Chỉ dùng cho card tự tạo. */
    @Column(columnDefinition = "TEXT")
    private String front;

    /** Chỉ dùng cho card tự tạo. */
    @Column(columnDefinition = "TEXT")
    private String back;

    /** Chỉ dùng cho card tự tạo. */
    @Column(length = 255)
    private String ipa;

    @Column(nullable = false)
    private UUID deckId;

    @Column(nullable = false)
    @Builder.Default
    private double stability = 0;

    @Column(nullable = false)
    @Builder.Default
    private double difficulty = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CardState state = CardState.NEW;

    private Instant lastReview;

    /** Card mới đến hạn ngay khi tạo. */
    @Column(nullable = false)
    @Builder.Default
    private Instant nextReview = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int lapseCount = 0;

    @Column(columnDefinition = "TEXT")
    private String contextSentence;

    private UUID sourceVideoId;
    private UUID sourceSegmentId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CardSource sourceType;

    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isCustom() {
        return vocab == null;
    }

    public String resolvedFront() {
        return isCustom() ? front : vocab.getWord();
    }

    public String resolvedBack() {
        return isCustom() ? back : vocab.getDefinition();
    }

    public String resolvedIpa() {
        return isCustom() ? ipa : vocab.getIpa();
    }
}
