package com.step.dictionary.standard.word.dto;

import com.step.dictionary.standard.word.Word;
import java.time.LocalDateTime;

/**
 * 단어 응답. 엔티티를 직접 노출하지 않고 이 record로 변환해 내려준다.
 *
 * @param id           단어 ID
 * @param name         단어명
 * @param abbreviation 영문약어 (대문자)
 * @param englishName  영문명
 * @param description  설명 (null 가능)
 * @param classifier   분류어 여부
 * @param createdAt    생성 일시
 * @param updatedAt    최종 수정 일시
 */
public record WordResponse(
        Long id,
        String name,
        String abbreviation,
        String englishName,
        String description,
        boolean classifier,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 엔티티를 응답으로 변환한다. */
    public static WordResponse from(Word word) {
        return new WordResponse(
                word.getId(),
                word.getName(),
                word.getAbbreviation(),
                word.getEnglishName(),
                word.getDescription(),
                word.isClassifier(),
                word.getCreatedAt(),
                word.getUpdatedAt());
    }
}
