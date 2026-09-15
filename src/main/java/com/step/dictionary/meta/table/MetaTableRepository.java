package com.step.dictionary.meta.table;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 테이블 정의 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link MetaTableRepositoryCustom}이 맡는다.
 */
public interface MetaTableRepository extends JpaRepository<MetaTable, Long>, MetaTableRepositoryCustom {

    /** 등록 시 물리명 중복 확인. 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByPhysicalName(String physicalName);

    /** 수정 시 물리명 중복 확인. 자기 자신(id)은 제외하며 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByPhysicalNameAndIdNot(String physicalName, Long id);
}
