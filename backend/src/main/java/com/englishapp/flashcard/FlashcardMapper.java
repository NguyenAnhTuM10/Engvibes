package com.englishapp.flashcard;

import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.vocab.VocabMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VocabMapper.class})
public interface FlashcardMapper {

    @Mapping(source = "vocab", target = "vocab")
    CardResponse toCardResponse(UserCard card);
}
