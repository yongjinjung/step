package com.step.dictionary.standard.word;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WordTest {

    @Test
    @DisplayName("영문약어는 대문자로 저장된다")
    void normalizeAbbreviationOnCreate() {
        Word word = new Word("고객", "cust", "Customer", null, false);

        assertThat(word.getAbbreviation()).isEqualTo("CUST");
    }

    @Test
    @DisplayName("수정 시에도 영문약어는 대문자로 저장된다")
    void update() {
        Word word = new Word("고객", "CUST", "Customer", null, false);

        word.update("번호", "no", "Number", "식별 번호", true);

        assertThat(word.getName()).isEqualTo("번호");
        assertThat(word.getAbbreviation()).isEqualTo("NO");
        assertThat(word.getEnglishName()).isEqualTo("Number");
        assertThat(word.getDescription()).isEqualTo("식별 번호");
        assertThat(word.isClassifier()).isTrue();
    }
}
