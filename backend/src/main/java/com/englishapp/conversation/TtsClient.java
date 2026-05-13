package com.englishapp.conversation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class TtsClient {

    private final WebClient webClient;

    public TtsClient(
            @Value("${app.openai.base-url}") String baseUrl,
            @Value("${app.openai.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    public byte[] synthesize(String text, String voice) {
        log.info("TTS synthesize {} chars voice={}", text.length(), voice);
        return webClient.post()
                .uri("/audio/speech")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", "tts-1",
                        "input", text,
                        "voice", voice,
                        "response_format", "mp3"
                ))
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
    }
}
