package com.englishapp.seed;

import com.englishapp.flashcard.*;
import com.englishapp.recommend.*;
import com.englishapp.session.*;
import com.englishapp.shadow.*;
import com.englishapp.stats.*;
import com.englishapp.user.*;
import com.englishapp.video.*;
import com.englishapp.vocab.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoSeedRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final SessionRepository sessionRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final VocabRepository vocabRepository;
    private final UserPhonemeStatsRepository phonemeStatsRepository;
    private final UserEventRepository userEventRepository;
    private final UserVideoInteractionRepository interactionRepository;
    private final PasswordEncoder passwordEncoder;

    // {title, topic, cefrLevel, summary, speakingQuestion}
    private static final String[][] VIDEO_DATA = {
        {"How Smartphones Changed Communication", "technology", "B1",
         "Smartphones have fundamentally transformed how people communicate, work, and access information in the modern world.",
         "How has smartphone use affected face-to-face communication in your daily life?"},
        {"Artificial Intelligence in Everyday Life", "technology", "B2",
         "This video examines the growing presence of AI in daily tasks, from voice assistants to recommendation systems.",
         "What are the potential risks of relying too heavily on artificial intelligence?"},
        {"Basic Computer Skills for Beginners", "technology", "A2",
         "A beginner-friendly guide to using computers, covering essential skills like typing, browsing, and file management.",
         "What basic computer skills do you think are most important to learn first?"},
        {"The Future of Electric Vehicles", "technology", "B1",
         "Electric vehicles are reshaping the automotive industry and reducing dependence on fossil fuels.",
         "Would you consider buying an electric vehicle? What factors would influence your decision?"},
        {"Climate Change and Its Effects", "environment", "B1",
         "Climate change is causing rising temperatures, extreme weather events, and sea level rises worldwide.",
         "What steps can individuals take to reduce their carbon footprint?"},
        {"Protecting Ocean Biodiversity", "environment", "B2",
         "Marine ecosystems face unprecedented threats from pollution, overfishing, and climate change.",
         "Why is it important to protect ocean biodiversity, and what can governments do?"},
        {"Simple Ways to Reduce Plastic Waste", "environment", "A2",
         "Practical tips for reducing everyday plastic use to protect the environment.",
         "What simple changes can you make at home to use less plastic?"},
        {"Renewable Energy Sources Explained", "environment", "B1",
         "An overview of solar, wind, and hydroelectric power as alternatives to fossil fuels.",
         "Which renewable energy source do you think has the most potential for your country?"},
        {"The Importance of Sleep for Health", "health", "A2",
         "Quality sleep is essential for physical health, mental wellbeing, and daily performance.",
         "How many hours of sleep do you get each night, and how does it affect you?"},
        {"Understanding Mental Health", "health", "B1",
         "Mental health awareness is growing globally, but stigma and lack of resources remain challenges.",
         "How can workplaces better support employees with mental health challenges?"},
        {"Healthy Eating on a Budget", "health", "B1",
         "Eating nutritiously does not have to be expensive; smart shopping and meal planning make it affordable.",
         "What are some affordable healthy foods you enjoy eating regularly?"},
        {"The Rise of Telemedicine", "health", "B2",
         "Digital healthcare platforms are making medical consultations more accessible and convenient.",
         "What are the advantages and disadvantages of seeing a doctor online?"},
        {"Starting a Small Business", "business", "B1",
         "Entrepreneurship requires planning, persistence, and the ability to adapt to changing market conditions.",
         "What challenges do you think small business owners face most often?"},
        {"Global Trade and Its Impact", "business", "B2",
         "International trade connects economies but also creates dependencies and inequality between nations.",
         "How does global trade affect workers in developing countries?"},
        {"Personal Finance Basics", "business", "B1",
         "Managing income, expenses, and savings effectively is a fundamental life skill.",
         "What financial habits do you think young people should develop early?"},
        {"Marketing in the Digital Age", "business", "B2",
         "Social media and digital platforms have transformed how companies reach and engage customers.",
         "How do you think targeted advertising affects consumer behavior?"},
        {"The Benefits of Online Learning", "education", "A2",
         "Online education offers flexibility and access to knowledge for learners worldwide.",
         "What do you prefer about online learning compared to traditional classrooms?"},
        {"Critical Thinking in Schools", "education", "B1",
         "Teaching students to question, analyze, and evaluate information is more important than ever.",
         "How can teachers better encourage critical thinking in the classroom?"},
        {"The Role of Libraries Today", "education", "B1",
         "Libraries have evolved beyond books to become community centers for learning and digital access.",
         "How do you think libraries should change to stay relevant in the digital age?"},
        {"Learning a Second Language", "education", "B2",
         "Bilingualism offers cognitive, cultural, and career advantages in an increasingly connected world.",
         "What strategies have you found most effective for learning a foreign language?"},
        {"Traveling on a Tight Budget", "travel", "A2",
         "Budget travel tips help you explore the world without spending a fortune.",
         "What is the best travel tip you would give someone visiting your country?"},
        {"Cultural Differences Around the World", "travel", "A2",
         "Understanding cultural differences helps travelers communicate respectfully and connect authentically.",
         "What cultural differences have you noticed when meeting people from other countries?"},
        {"Sustainable Tourism Practices", "travel", "B1",
         "Responsible tourism minimizes environmental impact and supports local communities.",
         "How can tourists be more environmentally responsible when they travel?"},
        {"City vs Rural Living", "travel", "B1",
         "Both urban and rural lifestyles offer unique advantages and challenges for residents.",
         "Would you prefer to live in a city or the countryside? Explain your reasons."},
        {"Traditional Foods Around the World", "food", "A2",
         "Every culture has distinctive dishes that reflect its history, climate, and values.",
         "What is a traditional food from your culture that you would recommend to visitors?"},
        {"The Farm-to-Table Movement", "food", "B1",
         "Eating locally sourced food supports farmers, reduces emissions, and often tastes better.",
         "How important is it to you to know where your food comes from?"},
        {"Food Waste and Its Solutions", "food", "B1",
         "A third of all food produced globally is wasted, with serious economic and environmental consequences.",
         "What can individuals do to reduce food waste in their daily lives?"},
        {"Street Food Culture in Asia", "food", "B2",
         "Street food markets in Asian cities blend culinary tradition, social interaction, and economic opportunity.",
         "How does street food culture reflect the broader culture of a city or country?"},
        {"The History of the Olympics", "sports", "A2",
         "The Olympic Games have a rich history spanning ancient Greece to the modern international competition.",
         "What do you think is the most important value the Olympic Games represent?"},
        {"Mental Strength in Sports", "sports", "B1",
         "Elite athletes train their minds as rigorously as their bodies to perform under pressure.",
         "How do you think mental preparation affects athletic performance?"},
        {"Women in Professional Sports", "sports", "B1",
         "Female athletes continue to break barriers and challenge stereotypes in competitive sports worldwide.",
         "What more can be done to promote gender equality in professional sports?"},
        {"The Business of Football", "sports", "B2",
         "Modern football is a global industry involving billions of dollars in transfers, broadcasting, and sponsorship.",
         "Do you think commercial interests have had a positive or negative effect on football?"},
        {"How the Brain Works", "science", "B1",
         "Neuroscience is uncovering the mysteries of human cognition, memory, and emotion.",
         "What aspect of how the brain works do you find most fascinating and why?"},
        {"Space Exploration Today", "science", "B2",
         "Private companies and government agencies are racing to return to the Moon and eventually reach Mars.",
         "Should governments spend money on space exploration or focus on problems on Earth?"},
        {"Genetics and Heredity Explained", "science", "B2",
         "DNA carries the genetic code that determines traits passed from parents to children.",
         "What ethical questions does the ability to edit genes raise for society?"},
        {"The Science of Happiness", "science", "B1",
         "Psychological research reveals practical habits that consistently increase wellbeing and life satisfaction.",
         "What daily habits do you practice that contribute most to your happiness?"},
        {"Classical Music for Beginners", "arts", "A2",
         "An introduction to famous composers and pieces that have shaped Western musical tradition.",
         "What type of music do you find most relaxing or inspiring, and why?"},
        {"The Influence of Street Art", "arts", "B1",
         "Graffiti and murals have evolved from vandalism to respected forms of cultural expression.",
         "Should street art be considered a legitimate art form? Give your reasons."},
        {"Cinema as a Mirror of Society", "arts", "B1",
         "Films reflect and shape the values, fears, and aspirations of the societies that produce them.",
         "What film has had the biggest impact on how you see the world?"},
        {"The Art of Storytelling", "arts", "B2",
         "Narrative techniques used in literature, film, and oral tradition reveal universal human experiences.",
         "Why do you think storytelling is such a fundamental part of human culture?"},
    };

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.existsByEmail("demo@englishapp.com")) {
            log.info("[DemoSeed] Already seeded — skipping");
            return;
        }
        log.info("[DemoSeed] Starting demo data seed...");

        User demoUser = createDemoUser();
        createAdmins();
        createRegularUsers();

        List<Video> videos = createVideos();
        FlashcardDeck deck = createDeck(demoUser);
        createCards(demoUser, deck, videos);
        createSessions(demoUser, videos);
        createInteractions(demoUser, videos);
        createEvents(demoUser, videos);
        createPhonemeStats(demoUser);

        demoUser.setTotalXp(875);
        demoUser.setCurrentStreakDays(7);
        demoUser.setLastActiveDate(LocalDate.now());
        userRepository.save(demoUser);

        log.info("[DemoSeed] Done — login: demo@englishapp.com / demo123");
    }

    private User createDemoUser() {
        return userRepository.save(User.builder()
                .email("demo@englishapp.com")
                .username("demo_user")
                .passwordHash(passwordEncoder.encode("demo123"))
                .cefrLevel(CEFRLevel.B1)
                .workingCefrLevel(CEFRLevel.B1)
                .role(Role.USER)
                .build());
    }

    private void createAdmins() {
        for (int i = 1; i <= 3; i++) {
            userRepository.save(User.builder()
                    .email("admin" + i + "@englishapp.com")
                    .username("admin_" + i)
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .cefrLevel(CEFRLevel.C1)
                    .workingCefrLevel(CEFRLevel.C1)
                    .role(Role.ADMIN)
                    .build());
        }
    }

    private void createRegularUsers() {
        CEFRLevel[] levels = {CEFRLevel.A2, CEFRLevel.A2, CEFRLevel.A2, CEFRLevel.B1, CEFRLevel.B1,
                              CEFRLevel.B1, CEFRLevel.B2, CEFRLevel.B2, CEFRLevel.B2};
        for (int i = 1; i <= 9; i++) {
            userRepository.save(User.builder()
                    .email("learner" + String.format("%02d", i) + "@englishapp.com")
                    .username("learner_" + String.format("%02d", i))
                    .passwordHash(passwordEncoder.encode("learner123"))
                    .cefrLevel(levels[i - 1])
                    .workingCefrLevel(levels[i - 1])
                    .role(Role.USER)
                    .build());
        }
    }

    private List<Video> createVideos() {
        String warmupWords = "[{\"word\":\"essential\",\"ipa\":\"ɪˈsenʃəl\",\"definition\":\"absolutely necessary\",\"cefrLevel\":\"B1\",\"partOfSpeech\":\"adjective\"},{\"word\":\"impact\",\"ipa\":\"ˈɪmpækt\",\"definition\":\"a strong effect\",\"cefrLevel\":\"B1\",\"partOfSpeech\":\"noun\"}]";
        String collocations = "{\"essential\":[\"essential skill\",\"essential role\",\"play an essential part\"],\"impact\":[\"have an impact\",\"major impact\",\"positive impact\"]}";

        List<Video> videos = new ArrayList<>();
        for (int i = 0; i < VIDEO_DATA.length; i++) {
            String[] d = VIDEO_DATA[i];
            CEFRLevel cefr = CEFRLevel.valueOf(d[2]);
            String keyPoints = String.format(
                    "[\"%s.\",\"This topic is relevant to %s learners.\",\"Practice speaking about this subject regularly.\"]",
                    d[3].substring(0, Math.min(60, d[3].length())), d[2]);
            Video v = Video.builder()
                    .id(UUID.randomUUID())
                    .title(d[0])
                    .topic(d[1])
                    .cefrLevel(cefr)
                    .description(d[3])
                    .summary(d[3])
                    .keyPointsJson(keyPoints)
                    .speakingQuestion(d[4])
                    .warmupWordsJson(warmupWords)
                    .collocationsJson(collocations)
                    .storageUrl("demo/videos/placeholder.mp4")
                    .thumbnailUrl("demo/thumbnails/placeholder.jpg")
                    .durationSec(30 + (i % 30))
                    .status(VideoStatus.PUBLISHED)
                    .vocabDifficulty(0.3 + (i % 5) * 0.1)
                    .popularityScore(0.5 + (i % 10) * 0.05)
                    .viewCount(10 + i * 3)
                    .build();
            videos.add(videoRepository.save(v));
        }
        log.info("[DemoSeed] Created {} videos", videos.size());
        return videos;
    }

    private FlashcardDeck createDeck(User user) {
        return deckRepository.save(FlashcardDeck.builder()
                .userId(user.getId())
                .name("My Vocabulary")
                .color("#3B82F6")
                .isDefault(true)
                .build());
    }

    private void createCards(User user, FlashcardDeck deck, List<Video> videos) {
        List<VocabEntry> vocab = vocabRepository.findAll(PageRequest.of(0, 100)).getContent();
        if (vocab.isEmpty()) {
            log.warn("[DemoSeed] No vocab entries found — skipping cards");
            return;
        }
        Instant now = Instant.now();
        List<UserCard> cards = new ArrayList<>();
        for (int i = 0; i < Math.min(100, vocab.size()); i++) {
            VocabEntry v = vocab.get(i);
            CardState state = i < 30 ? CardState.REVIEW : (i < 60 ? CardState.LEARNING : CardState.NEW);
            Instant nextReview = switch (state) {
                case REVIEW -> now.minus(1 + i % 3, ChronoUnit.DAYS);
                case LEARNING -> now.plus(1 + i % 7, ChronoUnit.DAYS);
                default -> now.plus(30 + i % 30, ChronoUnit.DAYS);
            };
            cards.add(UserCard.builder()
                    .userId(user.getId())
                    .vocab(v)
                    .deckId(deck.getId())
                    .state(state)
                    .stability(state == CardState.REVIEW ? 5.0 + i % 10 : 1.0)
                    .difficulty(0.3 + (i % 5) * 0.1)
                    .reviewCount(state == CardState.NEW ? 0 : 1 + i % 8)
                    .lastReview(state == CardState.NEW ? null : now.minus(i % 14, ChronoUnit.DAYS))
                    .nextReview(nextReview)
                    .sourceVideoId(videos.get(i % videos.size()).getId())
                    .sourceType(CardSource.WARMUP)
                    .build());
        }
        cardRepository.saveAll(cards);
        log.info("[DemoSeed] Created {} cards", cards.size());
    }

    private void createSessions(User user, List<Video> videos) {
        Instant now = Instant.now();
        List<LearningSession> sessions = new ArrayList<>();
        int count = Math.min(25, videos.size());
        for (int i = 0; i < count; i++) {
            Instant completedAt = now.minus(count - i, ChronoUnit.DAYS);
            boolean fullCompletion = i % 5 != 0;
            String completedSteps = fullCompletion ? "[0,1,2,3,4,5,6]" : "[0,1,2,3]";
            int xp = fullCompletion ? 35 : 20;
            sessions.add(LearningSession.builder()
                    .userId(user.getId())
                    .videoId(videos.get(i).getId())
                    .currentStep(7)
                    .completedSteps(completedSteps)
                    .status(SessionStatus.COMPLETED)
                    .scaffoldLevel(1 + i % 4)
                    .totalXpEarned(xp)
                    .startedAt(completedAt.minus(45, ChronoUnit.MINUTES))
                    .completedAt(completedAt)
                    .build());
        }
        sessionRepository.saveAll(sessions);
        log.info("[DemoSeed] Created {} sessions", sessions.size());
    }

    private void createInteractions(User user, List<Video> videos) {
        Instant now = Instant.now();
        List<UserVideoInteraction> interactions = new ArrayList<>();
        int count = Math.min(25, videos.size());
        for (int i = 0; i < count; i++) {
            boolean full = i % 5 != 0;
            interactions.add(UserVideoInteraction.builder()
                    .id(new UserVideoInteractionId(user.getId(), videos.get(i).getId()))
                    .viewCount(1 + i % 3)
                    .completionScore(full ? 1.0 : 0.5)
                    .lastViewed(now.minus(count - i, ChronoUnit.DAYS))
                    .build());
        }
        interactionRepository.saveAll(interactions);
    }

    private void createEvents(User user, List<Video> videos) {
        String[] types = {"VIDEO_WATCH", "SESSION_START", "SESSION_COMPLETE", "PHRASE_ATTEMPT", "SHADOW_ATTEMPT", "RETELL_ATTEMPT"};
        List<UserEvent> events = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String type = types[i % types.length];
            String payload = String.format("{\"videoId\":\"%s\",\"step\":%d}",
                    videos.get(i % videos.size()).getId(), i % 7);
            events.add(UserEvent.builder()
                    .userId(user.getId())
                    .eventType(type)
                    .payload(payload)
                    .createdAt(Instant.now().minus(50 - i, ChronoUnit.HOURS))
                    .build());
        }
        userEventRepository.saveAll(events);
        log.info("[DemoSeed] Created 50 events");
    }

    private void createPhonemeStats(User user) {
        // Common weak phonemes for Vietnamese learners
        Object[][] phonemes = {
            {"TH",  55, 33},  // /θ/ — voiceless th
            {"DH",  48, 28},  // /ð/ — voiced th
            {"R",   62, 28},  // /ɹ/ — American r
            {"AE",  50, 22},  // /æ/ — short a (cat)
            {"NG",  40, 16},  // /ŋ/ — ng ending
            {"IH",  80, 12},  // /ɪ/ — short i (good)
            {"EY",  70,  8},  // /eɪ/ — long a (good)
        };
        List<UserPhonemeStats> stats = new ArrayList<>();
        for (Object[] p : phonemes) {
            stats.add(UserPhonemeStats.builder()
                    .id(new UserPhonemeStatsId(user.getId(), (String) p[0]))
                    .totalAttempts((int) p[1])
                    .errors((int) p[2])
                    .build());
        }
        phonemeStatsRepository.saveAll(stats);
        log.info("[DemoSeed] Created phoneme stats (weak: TH, DH, R, AE)");
    }
}
