package com.step.dictionary.meta.column;

import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 컬럼 정의 커스텀 리포지토리 (Querydsl 동적 쿼리). 구현은 {@link MetaColumnRepositoryCustomImpl}.
 */
public interface MetaColumnRepositoryCustom {

    /**
     * 비표준 컬럼만 검색한다. 조건은 {@link MetaColumn#inspect()}와 같은 규칙을 DB에서 적용한 것이다.
     * 결과는 테이블 물리명, 컬럼 순서(seq), ID 오름차순이며 테이블·용어·도메인을 함께 가져온다.
     *
     * @param condition 테이블 ID와 사유. 둘 다 선택
     * @param pageable  페이지 번호와 크기 (정렬은 고정)
     */
    Page<MetaColumn> searchNonStandard(NonStandardColumnCondition condition, Pageable pageable);
}
