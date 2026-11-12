package com.englishapp.flashcard;

import com.englishapp.flashcard.dto.CardResponse;
import com.englishapp.vocab.VocabMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {VocabMapper.class})
public interface FlashcardMapper {

    @Mapping(source = "vocab", target = "vocab")
    @Mapping(target = "front", expression = "java(card.resolvedFront())")
    @Mapping(target = "back", expression = "java(card.resolvedBack())")
    @Mapping(target = "ipa", expression = "java(card.resolvedIpa())")
    @Mapping(target = "custom", expression = "java(card.isCustom())")
    CardResponse toCardResponse(UserCard card);
}
