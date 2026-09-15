package com.step.dictionary.standard.word;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 표준 단어 리포지토리.
 *
 * <p>기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link WordRepositoryCustom}(Querydsl 구현은 {@link WordRepositoryCustomImpl})이 맡는다.
 * 아래 {@code existsBy...} 메서드는 메서드 이름으로 쿼리가 만들어지며, 서비스에서 unique 제약 위반(500) 전에
 * 중복을 먼저 확인해 409로 응답하는 데 쓴다.
 */
public interface WordRepository extends JpaRepository<Word, Long>, WordRepositoryCustom {

    /** 단어명으로 찾는다. 엑셀 업로드에서 도메인의 분류어와 용어의 구성 단어를 이름으로 지정할 때 쓴다. */
    Optional<Word> findByName(String name);

    /** 등록 시 단어명 중복 확인. */
    boolean existsByName(String name);

    /** 등록 시 영문약어 중복 확인. 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByAbbreviation(String abbreviation);

    /** 수정 시 단어명 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByNameAndIdNot(String name, Long id);

    /** 수정 시 영문약어 중복 확인. 자기 자신(id)은 제외하며 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByAbbreviationAndIdNot(String abbreviation, Long id);
}
