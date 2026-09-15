package com.step.dictionary.user;

import com.step.dictionary.user.dto.UserSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 사용자 커스텀 리포지토리 (Querydsl 동적 쿼리). 구현은 {@link AppUserRepositoryCustomImpl}.
 */
public interface AppUserRepositoryCustom {

    /**
     * 조건에 맞는 사용자를 아이디 오름차순으로 페이지 조회한다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬은 아이디 순으로 고정)
     */
    Page<AppUser> search(UserSearchCondition condition, Pageable pageable);
}
