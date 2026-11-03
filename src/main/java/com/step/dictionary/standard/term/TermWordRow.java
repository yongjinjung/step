package com.step.dictionary.standard.term;

import com.step.dictionary.standard.word.Word;

/**
 * 조인 테이블 {@code std_term_word}의 한 줄과 그 단어. {@link TermMapper#findWordsByTermIds} 결과로만 쓴다.
 * MyBatis가 기본 생성자로 만들고 필드를 채운다.
 */
public class TermWordRow {

    private Long termId;
    private int seq;
    private Word word;

    public Long getTermId() {
        return termId;
    }

    public int getSeq() {
        return seq;
    }

    public Word getWord() {
        return word;
    }
}
