package com.englishapp.conversation;

import com.englishapp.ai.LlmClient;
import com.englishapp.ai.WhisperClient;
import com.englishapp.ai.WhisperResult;
import com.englishapp.common.ApiException;
import com.englishapp.conversation.dto.*;
import com.englishapp.retell.RateLimitService;
import com.englishapp.storage.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ConversationService {

    private final ConversationSessionRepository sessionRepository;
    private final ConversationTurnRepository turnRepository;
    private final WhisperClient whisperClient;
    private final LlmClient llmClient;
    private final TtsClient ttsClient;
    private final StorageService storageService;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Value("${app.storage.bucket-recordings}")
    private String recordingsBucket;

    @Value("${app.openai.api-key}")
    private String openaiApiKey;

    private static final int DAILY_TURN_LIMIT = 30;
    private static final int MAX_TURNS = 8;
    private static final String TTS_VOICE = "alloy";
    private static final Duration AUDIO_EXPIRY = Duration.ofHours(1);

    private boolean isMockMode() {
        return "dummy".equals(openaiApiKey) || openaiApiKey == null || openaiApiKey.isBlank();
    }

    @Transactional(readOnly = true)
    public List<ScenarioResponse> getScenarios() {
        return Arrays.stream(ConversationScenario.values())
                .map(s -> new ScenarioResponse(s.name(), s.displayName, s.description, s.aiRole, s.userGoal, s.openingLine))
                .toList();
    }

    public ConversationSessionResponse startSession(UUID userId, String scenarioId) {
        ConversationScenario scenario = parseScenario(scenarioId);

        ConversationSession session = ConversationSession.builder()
                .userId(userId)
                .scenarioId(scenarioId)
                .build();
        session = sessionRepository.save(session);
        UUID sessionId = session.getId();

        // TTS the opening line (optional — graceful fallback if TTS fails)
        String aiAudioUrl = null;
        String aiAudioKey = String.format("conversations/%s/turn-0-ai.mp3", sessionId);
        try {
            byte[] audioBytes = ttsClient.synthesize(scenario.openingLine, TTS_VOICE);
            storageService.upload(recordingsBucket, aiAudioKey,
                    new ByteArrayInputStream(audioBytes), audioBytes.length, "audio/mpeg");
            aiAudioUrl = storageService.generatePresignedUrl(recordingsBucket, aiAudioKey, AUDIO_EXPIRY);
        } catch (Exception e) {
            log.warn("TTS failed for opening line, continuing text-only: {}", e.getMessage());
        }

        HintResponse hints = buildInitialHints(scenario);
        ConversationTurn openingTurn = ConversationTurn.builder()
                .sessionId(sessionId)
                .turnNumber(0)
                .aiText(scenario.openingLine)
                .aiAudioKey(aiAudioKey)
                .hintsJson(serializeHints(hints))
                .build();
        turnRepository.save(openingTurn);

        return ConversationSessionResponse.builder()
                .sessionId(sessionId)
                .scenarioId(scenarioId)
                .scenarioDisplayName(scenario.displayName)
                .aiRole(scenario.aiRole)
                .userGoal(scenario.userGoal)
                .firstAiText(scenario.openingLine)
                .firstAiAudioUrl(aiAudioUrl)
                .hints(hints)
                .build();
    }

    public ConversationTurnResponse processTurn(UUID sessionId, UUID userId, MultipartFile audio) {
        if (!rateLimitService.tryAcquire(userId, "conversation", DAILY_TURN_LIMIT)) {
            throw ApiException.badRequest("Daily conversation limit reached (30 turns/day). Try again tomorrow.");
        }

        ConversationSession session = requireOwned(sessionId, userId);
        if (!"ACTIVE".equals(session.getStatus())) {
            throw ApiException.badRequest("This conversation session has already ended.");
        }

        int turnNumber = session.getTotalTurns() + 1;
        boolean isLastTurn = turnNumber >= MAX_TURNS;
        ConversationScenario scenario = parseScenario(session.getScenarioId());

        try {
            byte[] audioBytes = audio.getBytes();
            String userAudioKey = String.format("conversations/%s/turn-%d-user.webm", sessionId, turnNumber);
            storageService.upload(recordingsBucket, userAudioKey,
                    new ByteArrayInputStream(audioBytes), audioBytes.length,
                    audio.getContentType() != null ? audio.getContentType() : "audio/webm");

            String userTranscript;
            AiTurnResult aiResult;

            if (isMockMode()) {
                userTranscript = "[mock] " + (audio.getOriginalFilename() != null
                        ? audio.getOriginalFilename() : "recorded audio");
                aiResult = buildMockAiTurnResult(scenario, turnNumber);
                log.info("Mock mode: returning stub response for turn {}", turnNumber);
            } else {
                WhisperResult whisperResult = whisperClient.transcribe(
                        audioBytes,
                        audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "audio.webm",
                        audio.getContentType());
                userTranscript = whisperResult != null && whisperResult.getText() != null
                        ? whisperResult.getText().trim() : "";

                List<ConversationTurn> previousTurns = turnRepository.findBySessionIdOrderByTurnNumber(sessionId);
                String history = buildHistory(previousTurns);
                String rawJson = llmClient.chatCompletion(
                        buildSystemPrompt(scenario, isLastTurn),
                        buildUserPrompt(history, userTranscript, isLastTurn));
                aiResult = parseAiTurnResult(rawJson);
            }

            String aiAudioKey = String.format("conversations/%s/turn-%d-ai.mp3", sessionId, turnNumber);
            String aiAudioUrl = null;
            if (!isMockMode()) {
                try {
                    byte[] aiAudioBytes = ttsClient.synthesize(aiResult.aiMessage(), TTS_VOICE);
                    storageService.upload(recordingsBucket, aiAudioKey,
                            new ByteArrayInputStream(aiAudioBytes), aiAudioBytes.length, "audio/mpeg");
                    aiAudioUrl = storageService.generatePresignedUrl(recordingsBucket, aiAudioKey, AUDIO_EXPIRY);
                } catch (Exception e) {
                    log.warn("TTS failed for turn {}, continuing text-only: {}", turnNumber, e.getMessage());
                }
            }

            ConversationTurn turn = ConversationTurn.builder()
                    .sessionId(sessionId)
                    .turnNumber(turnNumber)
                    .userAudioKey(userAudioKey)
                    .userTranscript(userTranscript)
                    .aiText(aiResult.aiMessage())
                    .aiAudioKey(aiAudioKey)
                    .hintsJson(serializeHints(aiResult.hints()))
                    .build();
            turnRepository.save(turn);

            session.setTotalTurns(turnNumber);
            if (isLastTurn) {
                session.setStatus("COMPLETED");
                session.setEndedAt(Instant.now());
                session.setXpEarned(turnNumber * 5);
            }
            sessionRepository.save(session);

            return ConversationTurnResponse.builder()
                    .turnNumber(turnNumber)
                    .userTranscript(userTranscript)
                    .aiText(aiResult.aiMessage())
                    .aiAudioUrl(aiAudioUrl)
                    .hints(aiResult.hints())
                    .isLastTurn(isLastTurn)
                    .build();

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Conversation turn failed session={}: {}", sessionId, e.getMessage(), e);
            throw ApiException.badRequest("Failed to process turn: " + e.getMessage());
        }
    }

    public ConversationEndResponse endSession(UUID sessionId, UUID userId) {
        ConversationSession session = requireOwned(sessionId, userId);

        if (!"COMPLETED".equals(session.getStatus())) {
            session.setStatus("COMPLETED");
            session.setEndedAt(Instant.now());
            session.setXpEarned(session.getTotalTurns() * 5);

            List<ConversationTurn> turns = turnRepository.findBySessionIdOrderByTurnNumber(sessionId);
            ConversationScenario scenario = parseScenario(session.getScenarioId());
            if (isMockMode()) {
                session.setSummary("{\"grammarErrors\":[],\"vocabHighlights\":[\"great\",\"interesting\",\"exactly\"],"
                        + "\"encouragement\":\"You kept the conversation going well — great effort!\","
                        + "\"topTip\":\"Try to use more varied vocabulary instead of repeating the same words.\"}");
            } else {
                try {
                    String summaryJson = llmClient.chatCompletion(
                            "You are an English tutor. Return only valid JSON, no markdown.",
                            buildSummaryPrompt(scenario, turns));
                    session.setSummary(extractJson(summaryJson));
                } catch (Exception e) {
                    log.warn("Summary generation failed: {}", e.getMessage());
                }
            }
            sessionRepository.save(session);
        }

        return buildEndResponse(session);
    }

    private String buildHistory(List<ConversationTurn> turns) {
        if (turns.isEmpty()) return "(conversation just started)";
        return turns.stream()
                .map(t -> {
                    StringBuilder sb = new StringBuilder();
                    if (t.getUserTranscript() != null && !t.getUserTranscript().isBlank()) {
                        sb.append("User: ").append(t.getUserTranscript()).append("\n");
                    }
                    sb.append("AI: ").append(t.getAiText());
                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildSystemPrompt(ConversationScenario scenario, boolean isLastTurn) {
        String prompt = scenario.systemPrompt;
        if (isLastTurn) {
            prompt += "\nThis is the FINAL exchange — wrap up the conversation naturally and warmly.";
        }
        return prompt;
    }

    private String buildUserPrompt(String history, String userTranscript, boolean isLastTurn) {
        String userMsg = userTranscript.isBlank()
                ? "(user didn't say anything — gently encourage them to speak)"
                : "\"" + userTranscript + "\"";
        return """
                CONVERSATION SO FAR:
                %s

                USER'S LATEST MESSAGE: %s

                Respond naturally as your character. Keep aiMessage short (1-3 sentences).
                Provide 4 keyword hints the user could use in their NEXT response.
                %s
                """.formatted(
                history,
                userMsg,
                isLastTurn ? "This is the last turn — conclude the conversation." : ""
        );
    }

    private String buildSummaryPrompt(ConversationScenario scenario, List<ConversationTurn> turns) {
        String userMessages = turns.stream()
                .filter(t -> t.getUserTranscript() != null && !t.getUserTranscript().isBlank())
                .map(t -> "Turn " + t.getTurnNumber() + ": " + t.getUserTranscript())
                .collect(Collectors.joining("\n"));

        return """
                Analyze this English conversation practice session. Scenario: %s.

                USER'S MESSAGES:
                %s

                Return JSON with this exact structure:
                {
                  "grammarErrors": [{"error": "<wrong phrase>", "correction": "<fixed phrase>"}],
                  "vocabHighlights": ["<good word or phrase the user used>"],
                  "encouragement": "<1 specific encouraging note about something they did well>",
                  "topTip": "<1 most important, actionable improvement tip>"
                }
                Max 3 grammar errors. Max 3 vocab highlights. Be supportive and specific.
                """.formatted(scenario.displayName, userMessages.isBlank() ? "(no user speech detected)" : userMessages);
    }

    private record AiTurnResult(String aiMessage, HintResponse hints) {}

    @SuppressWarnings("unchecked")
    private AiTurnResult parseAiTurnResult(String raw) {
        try {
            String json = extractJson(raw);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            String aiMessage = (String) map.get("aiMessage");
            if (aiMessage == null || aiMessage.isBlank()) aiMessage = raw;

            Map<String, Object> hintsMap = (Map<String, Object>) map.get("hints");
            if (hintsMap == null) return new AiTurnResult(aiMessage, HintResponse.empty());

            List<String> keywords = (List<String>) hintsMap.getOrDefault("keywords", List.of());
            String exampleSentence = (String) hintsMap.getOrDefault("exampleSentence", "");
            return new AiTurnResult(aiMessage, new HintResponse(keywords, exampleSentence));
        } catch (Exception e) {
            log.error("Failed to parse AI turn JSON, using raw text: {}", e.getMessage());
            // Fallback: treat entire response as AI message, no hints
            String fallback = raw.length() > 500 ? raw.substring(0, 500) : raw;
            return new AiTurnResult(fallback, HintResponse.empty());
        }
    }

    private AiTurnResult buildMockAiTurnResult(ConversationScenario scenario, int turnNumber) {
        String[] mockReplies = switch (scenario) {
            case JOB_INTERVIEW -> new String[]{
                "Great introduction! Could you tell me more about your technical skills?",
                "Interesting! What would you say is your greatest strength as an engineer?",
                "Can you describe a challenging project you worked on and how you handled it?",
                "How do you usually approach working in a team environment?",
                "Where do you see yourself professionally in the next two to three years?",
                "Excellent answers! Thanks so much for coming in today. We'll be in touch soon."
            };
            case COFFEE_SHOP -> new String[]{
                "Great choice! Would you like that for here or to go?",
                "Got it! What size would you like — small, medium, or large?",
                "Oh, I'm sorry! We're actually out of that right now. Can I get you something similar?",
                "Of course! What name should I put on the order?",
                "Your total is four dollars fifty. Will that be cash or card?",
                "Here's your drink! Enjoy and have a great day!"
            };
            case HOTEL_CHECKIN -> new String[]{
                "Of course! Could I have your last name, please?",
                "Perfect, I found your reservation. May I see your ID?",
                "Thank you! I'm afraid your room isn't quite ready yet. May I offer you a complimentary upgrade?",
                "Wonderful! Breakfast is served from seven to ten in our restaurant on the ground floor.",
                "Here's your room key. The elevator is just down the hall to your left.",
                "Enjoy your stay with us! Don't hesitate to call reception if you need anything."
            };
            case DOCTOR_APPOINTMENT -> new String[]{
                "I'm sorry to hear that. How long have you been feeling this way?",
                "I see. Have you experienced any other symptoms alongside that?",
                "That's quite common, don't worry. Are you currently taking any medication?",
                "I'd recommend getting plenty of rest and staying hydrated.",
                "I'll prescribe something mild to help. Is there anything else you'd like to ask?",
                "Take care and feel better soon! Come back if things don't improve."
            };
            case MAKING_PLANS -> new String[]{
                "Oh that sounds fun! What day were you thinking?",
                "Saturday works for me! Did you have something specific in mind?",
                "I'm not really into horror movies, to be honest. How about a comedy instead?",
                "Great idea! Do you know any good restaurants near the cinema?",
                "Perfect! Let's meet at noon and grab lunch first, then the movie.",
                "It's a plan! I'm really looking forward to it. See you Saturday!"
            };
        };
        String reply = turnNumber <= mockReplies.length
                ? mockReplies[turnNumber - 1]
                : "Thanks for sharing that! What would you like to talk about next?";

        HintResponse hints = switch (scenario) {
            case JOB_INTERVIEW -> new HintResponse(
                    List.of("passionate", "detail-oriented", "problem-solving", "collaborate"),
                    "I'm passionate about writing clean code and I enjoy collaborating with my team."
            );
            case COFFEE_SHOP -> new HintResponse(
                    List.of("medium", "oat milk", "to go", "no sugar"),
                    "I'll have a medium oat milk latte to go, please."
            );
            case HOTEL_CHECKIN -> new HintResponse(
                    List.of("reservation", "two nights", "king bed", "non-smoking"),
                    "Yes, I have a reservation for two nights — a king bed, non-smoking room."
            );
            case DOCTOR_APPOINTMENT -> new HintResponse(
                    List.of("since Monday", "mild pain", "no fever", "feeling tired"),
                    "It started on Monday — mild pain and I've been feeling quite tired."
            );
            case MAKING_PLANS -> new HintResponse(
                    List.of("Saturday afternoon", "hiking", "sounds great", "meet at noon"),
                    "Saturday afternoon sounds great! We could meet at noon and go from there."
            );
        };
        return new AiTurnResult(reply, hints);
    }

    private HintResponse buildInitialHints(ConversationScenario scenario) {
        return switch (scenario) {
            case JOB_INTERVIEW -> new HintResponse(
                    List.of("background", "experience", "passionate", "skills"),
                    "I have a background in software engineering with about 2 years of experience..."
            );
            case COFFEE_SHOP -> new HintResponse(
                    List.of("latte", "medium size", "to go", "oat milk"),
                    "I'd like a medium oat milk latte to go, please."
            );
            case HOTEL_CHECKIN -> new HintResponse(
                    List.of("reservation", "last name", "check-in", "two nights"),
                    "Yes, I have a reservation under the name Smith, for two nights."
            );
            case DOCTOR_APPOINTMENT -> new HintResponse(
                    List.of("headache", "since yesterday", "tired", "no fever"),
                    "I've been having a bad headache since yesterday, and I feel quite tired."
            );
            case MAKING_PLANS -> new HintResponse(
                    List.of("hiking", "Saturday", "free", "sounds fun"),
                    "I'm free on Saturday! Maybe we could go hiking if the weather is good?"
            );
        };
    }

    private ConversationEndResponse buildEndResponse(ConversationSession session) {
        ConversationEndResponse.SummaryData summaryData = null;
        if (session.getSummary() != null && !session.getSummary().isBlank()) {
            try {
                summaryData = objectMapper.readValue(
                        session.getSummary(), ConversationEndResponse.SummaryData.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize session summary", e);
            }
        }
        return ConversationEndResponse.builder()
                .sessionId(session.getId())
                .totalTurns(session.getTotalTurns())
                .xpEarned(session.getXpEarned())
                .summary(summaryData)
                .build();
    }

    private String serializeHints(HintResponse hints) {
        try {
            return objectMapper.writeValueAsString(hints);
        } catch (Exception e) {
            return "{}";
        }
    }

    private ConversationSession requireOwned(UUID sessionId, UUID userId) {
        ConversationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Conversation session not found"));
        if (!session.getUserId().equals(userId)) throw ApiException.forbidden("Access denied");
        return session;
    }

    private ConversationScenario parseScenario(String scenarioId) {
        try {
            return ConversationScenario.valueOf(scenarioId);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Unknown scenario: " + scenarioId);
        }
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('\n') + 1;
            int end = trimmed.lastIndexOf("```");
            if (start > 0 && end > start) return trimmed.substring(start, end).trim();
        }
        return trimmed;
    }
}
