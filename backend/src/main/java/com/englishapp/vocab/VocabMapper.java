package com.englishapp.vocab;

import com.englishapp.vocab.dto.VocabResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VocabMapper {
    VocabResponse toVocabResponse(VocabEntry entry);
}
