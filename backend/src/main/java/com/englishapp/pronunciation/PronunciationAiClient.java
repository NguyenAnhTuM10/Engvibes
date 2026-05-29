package com.englishapp.pronunciation;

import com.englishapp.pronunciation.dto.AnalyzeRequest;
import com.englishapp.pronunciation.dto.AnalyzeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class PronunciationAiClient {

    private final WebClient webClient;

    public PronunciationAiClient(
            @Value("${app.pronunciation-service.url:http://localhost:8000}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    /**
     * Gửi transcript + target text sang Python service để so sánh IPA và tính điểm.
     * targetIpa có thể null — Python sẽ tự tính.
     */
    public AnalyzeResult analyze(String transcript, String targetText, String targetIpa) {
        AnalyzeRequest req = new AnalyzeRequest(transcript, targetText, targetIpa);
        log.debug("Pronunciation analyze: target='{}' transcript='{}'", targetText, transcript);

        return webClient.post()
                .uri("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .bodyToMono(AnalyzeResult.class)
                .block();
    }
}
