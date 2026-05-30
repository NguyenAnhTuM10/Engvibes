package com.englishapp.conversation;

import com.englishapp.retell.RateLimitService;
import com.englishapp.security.JwtService;
import com.englishapp.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * WebSocket proxy: browser ↔ Spring Boot ↔ OpenAI Realtime API.
 *
 * T1.1 — Server-owned session config:
 *   Proxy gửi session.update (instructions + voice) NGAY khi nhận session.created
 *   từ OpenAI, không phụ thuộc vào client. Mọi session.update từ client bị DROP.
 *
 * T1.2 — Rate limiting hai tầng:
 *   (a) Hard cap thời lượng/session (default 5 phút): ScheduledExecutorService tự đóng.
 *   (b) Daily quota qua Redis: reuse RateLimitService, từ chối nếu vượt ngưỡng ngày.
 */
@Slf4j
@Component
public class ConversationProxyWebSocketHandler extends AbstractWebSocketHandler {

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.realtime-model}")
    private String realtimeModel;

    @Value("${app.openai.realtime-voice:alloy}")
    private String realtimeVoice;

    @Value("${app.conversation.max-session-seconds:300}")
    private int maxSessionSeconds;

    @Value("${app.conversation.daily-session-limit:10}")
    private int dailySessionLimit;

    private final JwtService                   jwtService;
    private final UserRepository               userRepository;
    private final RateLimitService             rateLimitService;
    private final ConversationRealtimeService  realtimeService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "conv-timer");
                t.setDaemon(true);
                return t;
            });

    // ── Per-session state ─────────────────────────────────────────────────
    private final Map<String, WebSocket>          openAiSessions = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder>      textBuffers    = new ConcurrentHashMap<>();
    private final Map<String, UUID>               userIds        = new ConcurrentHashMap<>();
    private final Map<String, Instant>            sessionStarts  = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> timers         = new ConcurrentHashMap<>();
    // T2: DB session id + server-observed transcript buffer per WS connection
    private final Map<String, UUID> dbSessionIds = new ConcurrentHashMap<>();
    private final Map<String, java.util.List<ConversationRealtimeService.TranscriptEntry>> transcriptBuffers =
            new ConcurrentHashMap<>();

    public ConversationProxyWebSocketHandler(JwtService jwtService,
                                             UserRepository userRepository,
                                             RateLimitService rateLimitService,
                                             ConversationRealtimeService realtimeService,
                                             com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.jwtService       = jwtService;
        this.userRepository   = userRepository;
        this.rateLimitService = rateLimitService;
        this.realtimeService  = realtimeService;
        this.objectMapper     = objectMapper;
    }

    // ── Connection lifecycle ──────────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token      = queryParam(session, "token");
        String scenarioId = queryParam(session, "scenario");

        // ── Auth ──────────────────────────────────────────────────────────
        if (token == null) {
            close(session, CloseStatus.POLICY_VIOLATION.withReason("Missing token"));
            return;
        }
        UUID userId;
        try {
            String email = jwtService.extractEmail(token);
            userId = userRepository.findByEmail(email)
                    .orElseThrow()
                    .getId();
        } catch (Exception e) {
            log.warn("[conv-proxy] Invalid JWT: {}", e.getMessage());
            close(session, CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
            return;
        }

        // ── Scenario ──────────────────────────────────────────────────────
        ConversationScenario scenario;
        try {
            scenario = ConversationScenario.valueOf(scenarioId);
        } catch (Exception e) {
            close(session, CloseStatus.BAD_DATA.withReason("Invalid scenario: " + scenarioId));
            return;
        }

        // ── T1.2 (b): Daily session quota ─────────────────────────────────
        if (!rateLimitService.tryAcquire(userId, "conversation-realtime", dailySessionLimit)) {
            log.warn("[conv-proxy] Daily limit reached user={}", userId);
            close(session, CloseStatus.POLICY_VIOLATION.withReason(
                    "Daily conversation limit reached. Try again tomorrow."));
            return;
        }

        // ── Init per-session state ─────────────────────────────────────────
        String sid = session.getId();
        textBuffers.put(sid, new StringBuilder());
        userIds.put(sid, userId);
        sessionStarts.put(sid, Instant.now());
        transcriptBuffers.put(sid, new java.util.concurrent.CopyOnWriteArrayList<>());

        // ── T2.1: persist ConversationSession (status=ACTIVE) ──────────────
        UUID dbSessionId = realtimeService.createSession(userId, scenario.name()).getId();
        dbSessionIds.put(sid, dbSessionId);
        log.info("[conv-proxy] Created DB session={} for sid={}", dbSessionId, sid);

        // Báo DB sessionId cho browser — frontend dùng để gọi /realtime-review.
        // Custom event app.* không phải event OpenAI; frontend filter theo prefix.
        try {
            session.sendMessage(new TextMessage(
                    "{\"type\":\"app.session_created\",\"sessionId\":\"" + dbSessionId + "\"}"));
        } catch (IOException e) {
            log.warn("[conv-proxy] Could not send app.session_created sid={}: {}", sid, e.getMessage());
        }

        // ── T1.2 (a): Hard time cap ───────────────────────────────────────
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            log.info("[conv-proxy] Session time cap reached sid={} ({}s)", sid, maxSessionSeconds);
            close(session, new CloseStatus(4008, "Session time limit reached"));
        }, maxSessionSeconds, TimeUnit.SECONDS);
        timers.put(sid, timer);

        connectToOpenAI(session, scenario);
    }

    // ── T1.1: Filter client → OpenAI ─────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        WebSocket openAiWs = openAiSessions.get(session.getId());
        if (openAiWs == null) return;

        // T1.1 — Strip sensitive fields from client session.update.
        // Client vẫn được set audio format (cần cho mic), nhưng instructions/voice/
        // tools/model do SERVER sở hữu → loại khỏi message trước khi forward.
        if (isSessionUpdate(payload)) {
            String sanitized = stripSensitiveFields(payload, session.getId());
            if (sanitized != null) openAiWs.sendText(sanitized, true);
            return;
        }

        openAiWs.sendText(payload, true);
    }

    /**
     * T1.1 — Xoá instructions/voice/tools/model khỏi session.update của client.
     * Trả về JSON đã sanitize, hoặc null nếu parse lỗi (an toàn: drop hẳn).
     */
    private String stripSensitiveFields(String payload, String sid) {
        try {
            var root = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(payload);
            var sess = root.get("session");
            if (sess instanceof com.fasterxml.jackson.databind.node.ObjectNode sessionNode) {
                boolean removed = false;
                for (String f : new String[]{"instructions", "voice", "tools", "model"}) {
                    if (sessionNode.remove(f) != null) removed = true;
                }
                // Nested: session.audio.output.voice (API shape mới)
                var audio = sessionNode.get("audio");
                if (audio instanceof com.fasterxml.jackson.databind.node.ObjectNode audioNode) {
                    var output = audioNode.get("output");
                    if (output instanceof com.fasterxml.jackson.databind.node.ObjectNode outNode) {
                        if (outNode.remove("voice") != null) removed = true;
                    }
                }
                if (removed) {
                    log.warn("[conv-proxy] Stripped sensitive fields from client session.update sid={}", sid);
                }
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("[conv-proxy] Could not sanitize session.update sid={}, dropping: {}", sid, e.getMessage());
            return null;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sid = session.getId();

        // Cancel the time cap timer
        ScheduledFuture<?> timer = timers.remove(sid);
        if (timer != null) timer.cancel(false);

        WebSocket openAiWs = openAiSessions.remove(sid);
        textBuffers.remove(sid);

        UUID uid = userIds.remove(sid);
        Instant start = sessionStarts.remove(sid);
        UUID dbSessionId = dbSessionIds.remove(sid);
        var transcripts = transcriptBuffers.remove(sid);

        // ── T2.1 + T2.2: persist turns + complete session từ data server-observed ──
        if (dbSessionId != null && uid != null) {
            try {
                realtimeService.completeSession(dbSessionId, uid,
                        transcripts != null ? transcripts : java.util.List.of());
            } catch (Exception e) {
                log.error("[conv-proxy] Failed to complete session={}: {}", dbSessionId, e.getMessage());
            }
        }

        if (uid != null && start != null) {
            long seconds = java.time.Duration.between(start, Instant.now()).getSeconds();
            log.info("[conv-proxy] Session closed sid={} user={} duration={}s status={}",
                    sid, uid, seconds, status.getCode());
        }

        if (openAiWs != null) {
            openAiWs.sendClose(WebSocket.NORMAL_CLOSURE, "session ended").exceptionally(e -> null);
        }
    }

    // ── OpenAI connection + T1.1: server-initiated session.update ─────────

    private void connectToOpenAI(WebSocketSession browserSession,
                                  ConversationScenario scenario) {
        String sid = browserSession.getId();
        String url = "wss://api.openai.com/v1/realtime?model=" + realtimeModel;

        httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + apiKey)
                .buildAsync(URI.create(url), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        openAiSessions.put(sid, webSocket);
                        webSocket.request(1);
                        log.info("[conv-proxy] OpenAI Realtime connected sid={} scenario={}",
                                sid, scenario.name());
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket,
                                                     CharSequence data, boolean last) {
                        StringBuilder buf = textBuffers.get(sid);
                        if (buf != null) {
                            buf.append(data);
                            if (last) {
                                String msg = buf.toString();
                                buf.setLength(0);
                                handleOpenAiEvent(msg, webSocket, browserSession, scenario);
                            }
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket,
                                                      int statusCode, String reason) {
                        openAiSessions.remove(sid);
                        close(browserSession, CloseStatus.NORMAL);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("[conv-proxy] OpenAI error sid={}: {}", sid, error.getMessage());
                        openAiSessions.remove(sid);
                        close(browserSession, CloseStatus.SERVER_ERROR);
                    }
                });
    }

    /**
     * Xử lý event từ OpenAI.
     * T1.1: khi nhận session.created → proxy gửi session.update với
     *       instructions + voice do server build, không chờ client.
     */
    private void handleOpenAiEvent(String msg, WebSocket openAiWs,
                                    WebSocketSession browserSession,
                                    ConversationScenario scenario) {
        // T1.1 — khi OpenAI xác nhận session, proxy chiếm quyền config ngay
        if (msg.contains("\"type\":\"session.created\"")) {
            sendServerSessionUpdate(openAiWs, scenario);
            log.info("[conv-proxy] Sent authoritative session.update sid={}", browserSession.getId());
        }

        // T2.2 — Observe transcript events server-side (authoritative).
        // User turn: input audio transcription completed.
        // AI turn:   response output audio transcript done.
        observeTranscript(browserSession.getId(), msg);

        // Log sự kiện quan trọng
        if (msg.contains("\"type\":\"error\"")) {
            log.error("[conv-proxy] OpenAI error event: {}",
                    msg.length() > 400 ? msg.substring(0, 400) : msg);
        } else if (msg.contains("\"type\":\"session.")) {
            log.debug("[conv-proxy] session event: {}",
                    msg.length() > 200 ? msg.substring(0, 200) : msg);
        }

        // Relay tất cả event xuống browser
        try {
            if (browserSession.isOpen()) {
                browserSession.sendMessage(new TextMessage(msg));
            }
        } catch (IOException e) {
            log.error("[conv-proxy] Forward to browser failed sid={}: {}",
                    browserSession.getId(), e.getMessage());
        }
    }

    /**
     * T2.2 — Gom transcript từ stream OpenAI vào buffer server-side.
     * Đây là transcript CHÍNH THỨC dùng để chấm điểm — client không can thiệp được.
     */
    private void observeTranscript(String sid, String msg) {
        var buffer = transcriptBuffers.get(sid);
        if (buffer == null) return;

        boolean isUserTurn = msg.contains("\"type\":\"conversation.item.input_audio_transcription.completed\"");
        boolean isAiTurn   = msg.contains("\"type\":\"response.output_audio_transcript.done\"");
        if (!isUserTurn && !isAiTurn) return;

        try {
            var node = objectMapper.readTree(msg);
            String transcript = node.path("transcript").asText("");
            if (transcript.isBlank()) return;

            String role = isUserTurn ? "user" : "assistant";
            buffer.add(new ConversationRealtimeService.TranscriptEntry(role, transcript.trim()));
            log.debug("[conv-proxy] Observed {} transcript sid={}: {}", role, sid,
                    transcript.length() > 60 ? transcript.substring(0, 60) + "…" : transcript);
        } catch (Exception e) {
            log.warn("[conv-proxy] Failed to parse transcript event sid={}: {}", sid, e.getMessage());
        }
    }

    /**
     * T1.1 — Gửi session.update với config server-owned, dùng API shape mới
     * (gpt-realtime-2): instructions + audio.output.voice. Đây là minimal PATCH —
     * chỉ field server sở hữu, merge với audio-format do client set sau đó.
     * Build bằng Jackson để escape JSON đúng (instructions có newline/quote).
     */
    private void sendServerSessionUpdate(WebSocket openAiWs, ConversationScenario scenario) {
        try {
            var session = objectMapper.createObjectNode();
            // GA Realtime API yêu cầu session.type trong MỌI session.update.
            session.put("type", "realtime");
            session.put("instructions", scenario.buildRealtimeInstructions());
            session.set("audio", objectMapper.createObjectNode()
                    .set("output", objectMapper.createObjectNode().put("voice", realtimeVoice)));

            var root = objectMapper.createObjectNode();
            root.put("type", "session.update");
            root.set("session", session);

            openAiWs.sendText(objectMapper.writeValueAsString(root), true).exceptionally(e -> {
                log.error("[conv-proxy] Failed to send session.update: {}", e.getMessage());
                return null;
            });
        } catch (Exception e) {
            log.error("[conv-proxy] Could not build server session.update: {}", e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Detect client session.update để block.
     * String matching cố ý — tránh JSON parse overhead cho mọi audio chunk.
     */
    private boolean isSessionUpdate(String payload) {
        return payload.contains("\"type\":\"session.update\"");
    }

    private String queryParam(WebSocketSession session, String name) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return null;
        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return null;
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try {
            if (session.isOpen()) session.close(status);
        } catch (IOException e) {
            log.debug("[conv-proxy] Close error: {}", e.getMessage());
        }
    }
}
