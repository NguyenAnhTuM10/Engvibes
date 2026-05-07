package com.englishapp.ai;

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
public class WhisperClient {

    private final WebClient webClient;

    public WhisperClient(
            @Value("${app.openai.base-url}") String baseUrl,
            @Value("${app.openai.api-key}") String apiKey) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    public WhisperResult transcribe(byte[] audioBytes, String filename) {
        return transcribe(audioBytes, filename, "audio/mpeg");
    }

    public WhisperResult transcribe(byte[] audioBytes, String filename, String contentType) {
        log.info("Sending {}KB audio to Whisper ({})", audioBytes.length / 1024, contentType);

        ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() { return filename; }
        };

        String mediaType = contentType != null && !contentType.isBlank() ? contentType : "audio/mpeg";
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", audioResource).contentType(MediaType.parseMediaType(mediaType));
        builder.part("model", "whisper-1");
        builder.part("response_format", "verbose_json");
        builder.part("timestamp_granularities[]", "word");

        WhisperResult result = webClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(WhisperResult.class)
                .block();

        log.info("Whisper returned {} words", result != null && result.getWords() != null ? result.getWords().size() : 0);
        return result;
    }
}
