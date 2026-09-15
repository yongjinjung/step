package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.history.HistoryTargetType;
import com.step.dictionary.standard.word.Word;

/**
 * 표준 단어 스냅샷.
 *
 * @param id           단어 ID
 * @param name         단어명
 * @param abbreviation 영문약어
 * @param englishName  영문명
 * @param description  설명
 * @param classifier   분류어 여부
 */
public record WordSnapshot(Long id, String name, String abbreviation, String englishName, String description,
        boolean classifier) implements HistorySnapshot {

    /** 엔티티의 현재 값으로 스냅샷을 뜬다. */
    public static WordSnapshot from(Word word) {
        return new WordSnapshot(word.getId(), word.getName(), word.getAbbreviation(), word.getEnglishName(),
                word.getDescription(), word.isClassifier());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.WORD;
    }
}
