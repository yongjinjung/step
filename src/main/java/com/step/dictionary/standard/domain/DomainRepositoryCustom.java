package com.step.dictionary.standard.domain;

import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 표준 도메인 커스텀 리포지토리 (Querydsl 동적 쿼리). 구현은 {@link DomainRepositoryCustomImpl}.
 */
public interface DomainRepositoryCustom {

    /**
     * 조건에 맞는 도메인을 도메인명 오름차순으로 페이지 조회한다. 분류어 단어를 함께 가져온다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬은 도메인명 순으로 고정)
     */
    Page<Domain> search(DomainSearchCondition condition, Pageable pageable);
}
