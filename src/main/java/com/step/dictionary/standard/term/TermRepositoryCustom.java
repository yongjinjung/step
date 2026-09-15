package com.step.dictionary.standard.term;

import com.step.dictionary.standard.term.dto.TermSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 표준 용어 커스텀 리포지토리 (Querydsl 동적 쿼리). 구현은 {@link TermRepositoryCustomImpl}.
 */
public interface TermRepositoryCustom {

    /**
     * 조건에 맞는 용어를 용어명 오름차순으로 페이지 조회한다.
     * 도메인은 함께 가져오고, 구성 단어는 접근할 때 배치로 가져온다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬은 용어명 순으로 고정)
     */
    Page<Term> search(TermSearchCondition condition, Pageable pageable);
}
