package com.step.dictionary.standard.term;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 표준 용어 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link TermRepositoryCustom}이 맡는다.
 */
public interface TermRepository extends JpaRepository<Term, Long>, TermRepositoryCustom {

    /** 용어명으로 찾는다. 엑셀 업로드에서 컬럼에 매핑할 용어를 이름으로 지정할 때 쓴다. */
    Optional<Term> findByName(String name);

    /** 등록 시 생성된 용어명 중복 확인. */
    boolean existsByName(String name);

    /** 등록 시 생성된 영문약어 중복 확인. */
    boolean existsByAbbreviation(String abbreviation);

    /** 수정 시 용어명 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByNameAndIdNot(String name, Long id);

    /** 수정 시 영문약어 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByAbbreviationAndIdNot(String abbreviation, Long id);

    /** 이 도메인을 쓰는 용어가 있는지 확인한다. 도메인 삭제·분류어 변경 차단에 쓴다. */
    boolean existsByDomainId(Long domainId);

    /**
     * 이 단어를 구성 단어로 쓰는 용어가 있는지 확인한다.
     * {@code words} 컬렉션을 조인해 {@code words.id}로 비교하는 쿼리가 생성된다.
     * 단어 삭제(WORD_IN_USE)와 단어명/약어 변경(WORD_USED_BY_TERM) 차단에 쓴다.
     */
    boolean existsByWordsId(Long wordId);
}
