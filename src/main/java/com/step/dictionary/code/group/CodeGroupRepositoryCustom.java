package com.step.dictionary.code.group;

import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 공통 코드 그룹 커스텀 리포지토리 (Querydsl 동적 쿼리).
 * Spring Data가 이름 규칙({@code 리포지토리명 + CustomImpl})으로 {@link CodeGroupRepositoryCustomImpl}을 찾아 연결한다.
 */
public interface CodeGroupRepositoryCustom {

    /**
     * 조건에 맞는 코드 그룹을 코드ID 오름차순으로 페이지 조회한다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬은 코드ID 순으로 고정)
     */
    Page<CodeGroup> search(CodeGroupSearchCondition condition, Pageable pageable);
}
