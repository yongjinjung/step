package com.step.dictionary.code.group;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공통 코드 그룹 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link CodeGroupRepositoryCustom}(Querydsl)이 맡는다.
 */
public interface CodeGroupRepository extends JpaRepository<CodeGroup, Long>, CodeGroupRepositoryCustom {

    /** 등록 시 코드ID 중복 확인. 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByCodeId(String codeId);

    /** 수정 시 코드ID 중복 확인. 자기 자신(id)은 제외하며 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByCodeIdAndIdNot(String codeId, Long id);
}
