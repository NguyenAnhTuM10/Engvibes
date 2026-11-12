package com.englishapp.flashcard;

import com.englishapp.user.CEFRLevel;
import com.englishapp.user.User;
import com.englishapp.vocab.VocabEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chạy toàn bộ Flyway migration trên Postgres thật, để Hibernate validate entity với schema
 * (ddl-auto=validate), và kiểm tra các query của CardRepository với cả card từ điển lẫn card tự tạo.
 * Cần Docker.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class CardRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired TestEntityManager em;
    @Autowired CardRepository cardRepository;

    UUID userId;
    UUID deckId;
    UserCard dictionaryCard;
    UserCard customCard;
    UserCard futureCard;

    @BeforeEach
    void setUp() {
        User user = em.persist(User.builder()
                .email(UUID.randomUUID() + "@test.dev").username("tester").passwordHash("x").build());
        userId = user.getId();
        deckId = em.persist(FlashcardDeck.builder().userId(userId).name("Mixed").build()).getId();

        VocabEntry vocab = em.persist(VocabEntry.builder()
                .word("Achieve").cefrLevel(CEFRLevel.B1).partOfSpeech("verb").definition("to succeed").build());

        dictionaryCard = em.persist(UserCard.builder().userId(userId).deckId(deckId).vocab(vocab)
                .nextReview(Instant.now().minus(2, ChronoUnit.DAYS)).build());
        customCard = em.persist(UserCard.builder().userId(userId).deckId(deckId)
                .front("Resilient").back("kiên cường").sourceType(CardSource.IMPORT).build());
        futureCard = em.persist(UserCard.builder().userId(userId).deckId(deckId)
                .front("vague").back("mơ hồ").nextReview(Instant.now().plus(3, ChronoUnit.DAYS)).build());
        em.flush();
        em.clear();
    }

    @Test
    void frontsInDeck_includeBothCardKinds_lowerCased() {
        assertThat(cardRepository.findFrontsLowerInDeck(deckId))
                .containsExactlyInAnyOrder("achieve", "resilient", "vague");
    }

    @Test
    void dueCards_includeNewCustomCards_oldestFirst_andExcludeFutureOnes() {
        List<UserCard> due = cardRepository.findDueInDeck(deckId, Instant.now(), PageRequest.of(0, 10));

        assertThat(due).extracting(UserCard::getId)
                .containsExactly(dictionaryCard.getId(), customCard.getId());
        assertThat(due.get(0).resolvedFront()).isEqualTo("Achieve");    // vocab fetched with the card
        assertThat(cardRepository.countByDeckIdAndNextReviewLessThanEqual(deckId, Instant.now())).isEqualTo(2);
    }

    @Test
    void customCardLookup_ignoresCase_andOnlyMatchesCustomCards() {
        assertThat(cardRepository.findFirstByDeckIdAndVocabIsNullAndFrontIgnoreCase(deckId, "RESILIENT"))
                .get().extracting(UserCard::getId).isEqualTo(customCard.getId());
        assertThat(cardRepository.findFirstByDeckIdAndVocabIsNullAndFrontIgnoreCase(deckId, "achieve"))
                .isEmpty();
    }

    @Test
    void deckPage_returnsAllCardsWithResolvedContent() {
        var page = cardRepository.findByDeckId(deckId, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(UserCard::resolvedBack)
                .containsExactlyInAnyOrder("to succeed", "kiên cường", "mơ hồ");
    }
}
