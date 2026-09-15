package com.step.dictionary.history;

import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 변경 이력 커스텀 리포지토리 (Querydsl 동적 쿼리). 구현은 {@link ChangeHistoryRepositoryCustomImpl}.
 */
public interface ChangeHistoryRepositoryCustom {

    /**
     * 조건에 맞는 이력을 최신순(ID 내림차순)으로 페이지 조회한다. 변경자를 함께 가져온다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬은 최신순으로 고정)
     */
    Page<ChangeHistory> search(ChangeHistorySearchCondition condition, Pageable pageable);
}
