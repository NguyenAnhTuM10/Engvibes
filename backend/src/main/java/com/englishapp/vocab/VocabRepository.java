package com.englishapp.vocab;

import com.englishapp.user.CEFRLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VocabRepository extends JpaRepository<VocabEntry, UUID> {
    Optional<VocabEntry> findByWordAndPartOfSpeech(String word, String partOfSpeech);
    List<VocabEntry> findByCefrLevel(CEFRLevel cefrLevel);
    boolean existsByWord(String word);
    List<VocabEntry> findByWordIn(List<String> words);
    Optional<VocabEntry> findFirstByWordIgnoreCase(String word);
    Page<VocabEntry> findByWordContainingIgnoreCase(String word, Pageable pageable);
}
