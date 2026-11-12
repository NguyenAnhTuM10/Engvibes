package com.englishapp.flashcard;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V21 gộp SM-2 vào FSRS. Test chạy migration tới V20, chèn dữ liệu SM-2 như production
 * có thể có, rồi chạy V21 và kiểm tra dữ liệu được chuyển đúng. Cần Docker.
 */
@Testcontainers(disabledWithoutDocker = true)
class MergeSm2MigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static JdbcTemplate jdbc;
    static final UUID USER_ID       = UUID.randomUUID();
    static final UUID OWNED_DECK    = UUID.randomUUID();
    static final UUID ORPHAN_DECK   = UUID.randomUUID();
    static final UUID LEGACY_CARD   = UUID.randomUUID();

    @BeforeAll
    static void migrateWithSm2Data() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(ds);

        flyway(ds, "20").migrate();

        jdbc.update("INSERT INTO users (id, email, username, password_hash) VALUES (?, 'u@test.dev', 'u', 'x')", USER_ID);

        // An FSRS card created before V21 without next_review (the "never due" bug)
        UUID fsrsDeck = UUID.randomUUID();
        UUID vocabId = UUID.randomUUID();   // vocab is seeded by the app, not by migrations
        jdbc.update("INSERT INTO vocab_entries (id, word, part_of_speech, cefr_level, definition) "
                  + "VALUES (?, 'achieve', 'verb', 'B1', 'to succeed')", vocabId);
        jdbc.update("INSERT INTO flashcard_decks (id, user_id, name, is_default) VALUES (?, ?, 'My Deck', true)", fsrsDeck, USER_ID);
        jdbc.update("INSERT INTO user_cards (id, user_id, vocab_id, deck_id) VALUES (?, ?, ?, ?)",
                LEGACY_CARD, USER_ID, vocabId, fsrsDeck);

        // SM-2 deck owned by the user, with a case-insensitive duplicate
        jdbc.update("INSERT INTO sm2_decks (id, name, owner_id) VALUES (?, 'IELTS words', ?)", OWNED_DECK, USER_ID);
        jdbc.update("INSERT INTO sm2_cards (deck_id, front, back, ipa, example_sentence) VALUES (?, 'robust', 'mạnh mẽ', 'roʊˈbʌst', 'A robust plan.')", OWNED_DECK);
        jdbc.update("INSERT INTO sm2_cards (deck_id, front, back) VALUES (?, 'Robust', 'duplicate')", OWNED_DECK);
        jdbc.update("INSERT INTO sm2_cards (deck_id, front, back) VALUES (?, 'vague', 'mơ hồ')", OWNED_DECK);

        // SM-2 deck without owner (demo mode) — cannot be migrated
        jdbc.update("INSERT INTO sm2_decks (id, name) VALUES (?, 'Anonymous')", ORPHAN_DECK);
        jdbc.update("INSERT INTO sm2_cards (deck_id, front, back) VALUES (?, 'lost', 'mất')", ORPHAN_DECK);

        flyway(ds, null).migrate();
    }

    private static Flyway flyway(DriverManagerDataSource ds, String target) {
        var config = Flyway.configure().dataSource(ds).locations("classpath:db/migration");
        if (target != null) config.target(target);
        return config.load();
    }

    @Test
    void ownedSm2DeckBecomesFlashcardDeck() {
        assertThat(jdbc.queryForObject("SELECT user_id FROM flashcard_decks WHERE id = ?", UUID.class, OWNED_DECK))
                .isEqualTo(USER_ID);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flashcard_decks WHERE id = ?", Integer.class, ORPHAN_DECK))
                .isZero();
    }

    @Test
    void ownedSm2CardsBecomeCustomCards_deduplicatedAndDueNow() {
        List<Map<String, Object>> cards = jdbc.queryForList(
                "SELECT front, back, ipa, context_sentence, vocab_id, state, source_type, next_review "
              + "FROM user_cards WHERE deck_id = ? ORDER BY front", OWNED_DECK);

        assertThat(cards).extracting(r -> r.get("front")).containsExactly("robust", "vague");
        Map<String, Object> robust = cards.get(0);
        assertThat(robust.get("back")).isEqualTo("mạnh mẽ");        // first-created duplicate wins
        assertThat(robust.get("ipa")).isEqualTo("roʊˈbʌst");
        assertThat(robust.get("context_sentence")).isEqualTo("A robust plan.");
        assertThat(cards).allSatisfy(r -> {
            assertThat(r.get("vocab_id")).isNull();
            assertThat(r.get("state")).isEqualTo("NEW");
            assertThat(r.get("source_type")).isEqualTo("IMPORT");
            assertThat(r.get("next_review")).isNotNull();
        });
    }

    @Test
    void legacyCardWithoutNextReview_isBackfilledSoItBecomesDue() {
        assertThat(jdbc.queryForObject(
                "SELECT next_review <= NOW() FROM user_cards WHERE id = ?", Boolean.class, LEGACY_CARD))
                .isTrue();
    }

    @Test
    void sm2TablesAreDropped() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name LIKE 'sm2_%'", Integer.class))
                .isZero();
    }

    @Test
    void customCardsMustHaveFrontAndBack_andAreUniquePerDeckIgnoringCase() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO user_cards (user_id, deck_id, front) VALUES (?, ?, 'only front')", USER_ID, OWNED_DECK))
                .hasMessageContaining("chk_user_cards_content");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO user_cards (user_id, deck_id, front, back) VALUES (?, ?, 'VAGUE', 'dup')", USER_ID, OWNED_DECK))
                .hasMessageContaining("uq_user_cards_custom_front");
    }
}
