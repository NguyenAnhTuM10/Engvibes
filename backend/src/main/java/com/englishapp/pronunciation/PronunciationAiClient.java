package com.englishapp.pronunciation;

import com.englishapp.pronunciation.dto.AnalyzeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class PronunciationAiClient {

    private final WebClient webClient;

    public PronunciationAiClient(
            @Value("${app.pronunciation-service.url:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    /**
     * Gửi audio bytes + target text sang Python service (multipart/form-data).
     * Python dùng wav2vec2 để phân tích phoneme trực tiếp — không qua Whisper.
     */
    public AnalyzeResult analyze(byte[] audioBytes, String contentType,
                                 String targetText, String targetIpa) {
        log.debug("Pronunciation analyze (wav2vec2): target='{}'", targetText);

        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override public String getFilename() { return "audio.webm"; }
        };
        builder.part("audio", audioResource)
               .contentType(MediaType.parseMediaType(contentType));
        builder.part("target_text", targetText);
        if (targetIpa != null && !targetIpa.isBlank()) {
            builder.part("target_ipa", targetIpa);
        }

        return webClient.post()
                .uri("/analyze")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(AnalyzeResult.class)
                .block();
    }
}
