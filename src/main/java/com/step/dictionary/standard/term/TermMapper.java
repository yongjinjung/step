package com.step.dictionary.standard.term;

import com.step.dictionary.standard.term.dto.TermSearchCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 표준 용어 SQL 매퍼. 용어 조회 결과에는 도메인(과 분류어)만 채워지고, 구성 단어는
 * {@link #findWordsByTermIds}로 따로 읽어 {@link TermRepository}가 붙인다.
 */
@Mapper
public interface TermMapper {

    Optional<Term> findById(Long id);

    Optional<Term> findByName(String name);

    /** 여러 용어의 구성 단어를 한 번에 읽는다 (용어 ID, 순번 순). */
    List<TermWordRow> findWordsByTermIds(@Param("termIds") Collection<Long> termIds);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    long countByAbbreviation(@Param("abbreviation") String abbreviation, @Param("excludeId") Long excludeId);

    long countByDomainId(Long domainId);

    /** 단어를 구성 단어로 쓰는 용어 수. */
    long countByWordId(Long wordId);

    List<Term> search(@Param("c") TermSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") TermSearchCondition condition);

    void insert(Term term);

    void update(Term term);

    /** 구성 단어 한 줄을 넣는다. */
    void insertWord(@Param("termId") Long termId, @Param("seq") int seq, @Param("wordId") Long wordId);

    void deleteWordsByTermId(Long termId);

    void deleteById(Long id);
}
