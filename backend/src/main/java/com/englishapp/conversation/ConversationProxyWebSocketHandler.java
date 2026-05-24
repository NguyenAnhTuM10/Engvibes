package com.englishapp.conversation;

import com.englishapp.security.JwtService;
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
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConversationProxyWebSocketHandler extends AbstractWebSocketHandler {

    @Value("${app.openai.api-key}")
    private String apiKey;

    @Value("${app.openai.realtime-model}")
    private String realtimeModel;

    private final JwtService jwtService;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Map<String, WebSocket> openAiSessions = new ConcurrentHashMap<>();
    private final Map<String, StringBuilder> textBuffers  = new ConcurrentHashMap<>();

    public ConversationProxyWebSocketHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token      = queryParam(session, "token");
        String scenarioId = queryParam(session, "scenario");

        if (token == null) {
            close(session, CloseStatus.POLICY_VIOLATION.withReason("Missing token"));
            return;
        }
        try {
            jwtService.validateToken(token);
        } catch (Exception e) {
            log.warn("Invalid WS JWT: {}", e.getMessage());
            close(session, CloseStatus.POLICY_VIOLATION.withReason("Invalid token"));
            return;
        }

        ConversationScenario scenario;
        try {
            scenario = ConversationScenario.valueOf(scenarioId);
        } catch (Exception e) {
            close(session, CloseStatus.BAD_DATA.withReason("Invalid scenario: " + scenarioId));
            return;
        }

        textBuffers.put(session.getId(), new StringBuilder());
        connectToOpenAI(session, scenario);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocket openAiWs = openAiSessions.get(session.getId());
        if (openAiWs != null) {
            openAiWs.sendText(message.getPayload(), true);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocket openAiWs = openAiSessions.remove(session.getId());
        textBuffers.remove(session.getId());
        if (openAiWs != null) {
            openAiWs.sendClose(WebSocket.NORMAL_CLOSURE, "session ended").exceptionally(e -> null);
        }
    }

    private void connectToOpenAI(WebSocketSession browserSession, ConversationScenario scenario) {
        String url       = "wss://api.openai.com/v1/realtime?model=" + realtimeModel;
        String sessionId = browserSession.getId();

        httpClient.newWebSocketBuilder()
                .header("Authorization", "Bearer " + apiKey)
                .buildAsync(URI.create(url), new WebSocket.Listener() {

                    @Override
                    public void onOpen(WebSocket webSocket) {
                        openAiSessions.put(sessionId, webSocket);
                        webSocket.request(1);
                        log.info("OpenAI Realtime connected — session={}", sessionId);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        StringBuilder buf = textBuffers.get(sessionId);
                        if (buf != null) {
                            buf.append(data);
                            if (last) {
                                String msg = buf.toString();
                                buf.setLength(0);
                                if (msg.contains("\"type\":\"error\"")) {
                                    log.error("OpenAI error event: {}", msg);
                                } else if (msg.contains("\"type\":\"session.")) {
                                    log.info("OpenAI session event: {}", msg.length() > 400 ? msg.substring(0, 400) : msg);
                                } else if (msg.contains("\"type\":\"response.")) {
                                    log.info("OpenAI response event: type={}", msg.substring(msg.indexOf("\"type\":\"") + 8, Math.min(msg.indexOf("\"type\":\"") + 50, msg.length())));
                                } else if (msg.contains("\"type\":\"input_audio")) {
                                    log.debug("OpenAI audio ack: type={}", msg.substring(msg.indexOf("\"type\":\"") + 8, Math.min(msg.indexOf("\"type\":\"") + 50, msg.length())));
                                }
                                try {
                                    if (browserSession.isOpen()) {
                                        browserSession.sendMessage(new TextMessage(msg));
                                    }
                                } catch (IOException e) {
                                    log.error("Error forwarding to browser: {}", e.getMessage());
                                }
                            }
                        }
                        webSocket.request(1);
                        return null;
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        openAiSessions.remove(sessionId);
                        close(browserSession, CloseStatus.NORMAL);
                        return null;
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        log.error("OpenAI Realtime error session={}: {}", sessionId, error.getMessage());
                        openAiSessions.remove(sessionId);
                        close(browserSession, CloseStatus.SERVER_ERROR);
                    }
                });
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
            log.debug("Close error: {}", e.getMessage());
        }
    }
}
