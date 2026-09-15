package com.step.dictionary.meta.table;

import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 테이블 정의 커스텀 리포지토리 (Querydsl 동적 쿼리). 구현은 {@link MetaTableRepositoryCustomImpl}.
 */
public interface MetaTableRepositoryCustom {

    /**
     * 조건에 맞는 테이블을 물리명 오름차순으로 페이지 조회한다.
     *
     * @param condition 검색 조건. 키워드가 비어 있으면 전체
     * @param pageable  페이지 번호와 크기 (정렬은 물리명 순으로 고정)
     */
    Page<MetaTable> search(MetaTableSearchCondition condition, Pageable pageable);
}
