package com.step.dictionary.standard.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 표준 도메인 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link DomainRepositoryCustom}이 맡는다.
 */
public interface DomainRepository extends JpaRepository<Domain, Long>, DomainRepositoryCustom {

    /** 도메인명으로 찾는다. 엑셀 업로드에서 용어의 도메인을 이름으로 지정할 때 쓴다. */
    Optional<Domain> findByName(String name);

    /** 등록 시 도메인명 중복 확인. */
    boolean existsByName(String name);

    /** 수정 시 도메인명 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * 이 단어를 분류어로 쓰는 도메인이 있는지 확인한다.
     * 단어 삭제(WORD_IN_USE)와 분류어 해제(CLASSIFIER_WORD_IN_USE)를 막는 데 쓴다.
     * 메서드 이름의 {@code ClassifierWordId}는 {@code classifierWord.id} 경로로 해석된다.
     */
    boolean existsByClassifierWordId(Long wordId);
}
