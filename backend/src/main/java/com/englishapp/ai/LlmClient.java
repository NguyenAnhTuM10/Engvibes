package com.englishapp.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmClient {

    private final WebClient webClient;
    private final String model;
    private final String audioModel;
    private final String realtimeModel;

    public LlmClient(
            @Value("${app.openai.base-url}") String baseUrl,
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.model}") String model,
            @Value("${app.openai.audio-model}") String audioModel,
            @Value("${app.openai.realtime-model}") String realtimeModel) {
        this.model = model;
        this.audioModel = audioModel;
        this.realtimeModel = realtimeModel;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                .build();
    }

    /** Standard text-based chat completion — used by Listening, Retell, video pipeline, etc. */
    public String chatCompletion(String systemPrompt, String userPrompt) {
        log.debug("LLM request — model={}", model);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.3
        );

        Map<?, ?> response = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null) throw new RuntimeException("LLM returned null response");

        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("LLM returned no choices");

        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        String content = (String) message.get("content");
        log.debug("LLM response length: {} chars", content != null ? content.length() : 0);
        return content;
    }

    /**
     * Speaking evaluation — sends audio directly to GPT Audio model.
     * Single API call: audio → transcript + IELTS scores + feedback JSON.
     * Whisper is NOT used here.
     */
    public String chatCompletionWithAudio(String systemPrompt, String textPrompt,
                                          byte[] audioBytes, String audioFormat) {
        String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
        ObjectMapper mapper = new ObjectMapper();

        ArrayNode userContent = mapper.createArrayNode();

        ObjectNode audioItem = mapper.createObjectNode();
        audioItem.put("type", "input_audio");
        ObjectNode inputAudio = mapper.createObjectNode();
        inputAudio.put("data", base64Audio);
        inputAudio.put("format", audioFormat);
        audioItem.set("input_audio", inputAudio);
        userContent.add(audioItem);

        ObjectNode textItem = mapper.createObjectNode();
        textItem.put("type", "text");
        textItem.put("text", textPrompt);
        userContent.add(textItem);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode systemMsg = mapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        ObjectNode userMsg = mapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.set("content", userContent);
        messages.add(userMsg);

        ArrayNode modalities = mapper.createArrayNode();
        modalities.add("text");

        ObjectNode body = mapper.createObjectNode();
        body.put("model", audioModel);
        body.put("temperature", 0.3);
        body.set("modalities", modalities);
        body.set("messages", messages);

        log.info("Speaking eval — model={}, audioBytes={}, format={}", audioModel, audioBytes.length, audioFormat);

        Map<?, ?> response = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(), resp ->
                        resp.bodyToMono(String.class).map(err -> {
                            log.error("GPT Audio error [{}]: {}", resp.statusCode(), err);
                            return new RuntimeException("GPT Audio error " + resp.statusCode() + ": " + err);
                        }))
                .bodyToMono(Map.class)
                .block();

        if (response == null) throw new RuntimeException("GPT Audio returned null response");

        List<?> choices = (List<?>) response.get("choices");
        if (choices == null || choices.isEmpty()) throw new RuntimeException("GPT Audio returned no choices");

        Map<?, ?> message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
        String content = (String) message.get("content");
        log.debug("GPT Audio response length: {} chars", content != null ? content.length() : 0);
        return content;
    }

}
