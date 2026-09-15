package com.step.dictionary.standard.word;

import com.step.dictionary.standard.word.dto.WordSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 표준 단어 커스텀 리포지토리 (Querydsl 동적 쿼리).
 * Spring Data가 이름 규칙({@code 리포지토리명 + CustomImpl})으로 {@link WordRepositoryCustomImpl}을 찾아 연결한다.
 */
public interface WordRepositoryCustom {

    /**
     * 조건에 맞는 단어를 단어명 오름차순으로 페이지 조회한다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬 값은 쓰지 않고 단어명 순으로 고정한다)
     */
    Page<Word> search(WordSearchCondition condition, Pageable pageable);
}
