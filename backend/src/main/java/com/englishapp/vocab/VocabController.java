package com.englishapp.vocab;

import com.englishapp.common.ApiResponse;
import com.englishapp.common.ApiException;
import com.englishapp.vocab.dto.VocabResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Vocab", description = "Vocabulary lookup and search")
@RestController
@RequestMapping("/api/vocab")
@RequiredArgsConstructor
public class VocabController {

    private final VocabRepository vocabRepository;
    private final VocabMapper vocabMapper;

    @GetMapping("/search")
    public ApiResponse<Page<VocabResponse>> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<VocabResponse> results = vocabRepository
                .findByWordContainingIgnoreCase(q, PageRequest.of(page, Math.min(size, 50)))
                .map(vocabMapper::toVocabResponse);
        return ApiResponse.ok(results);
    }

    @GetMapping("/{id}")
    public ApiResponse<VocabResponse> getById(@PathVariable UUID id) {
        return ApiResponse.ok(
                vocabRepository.findById(id)
                        .map(vocabMapper::toVocabResponse)
                        .orElseThrow(() -> ApiException.notFound("Vocab not found"))
        );
    }

    @GetMapping("/by-word/{word}")
    public ApiResponse<VocabResponse> getByWord(@PathVariable String word) {
        return ApiResponse.ok(
                vocabRepository.findFirstByWordIgnoreCase(word)
                        .map(vocabMapper::toVocabResponse)
                        .orElseThrow(() -> ApiException.notFound("Word not found"))
        );
    }
}
