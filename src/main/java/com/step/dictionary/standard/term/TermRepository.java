package com.step.dictionary.standard.term;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.step.dictionary.global.mybatis.InClause;
import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.standard.term.dto.TermSearchCondition;
import com.step.dictionary.standard.word.Word;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 표준 용어 리포지토리 ({@link TermMapper} 사용).
 *
 * <p>용어와 구성 단어(조인 테이블 {@code std_term_word})를 한 묶음으로 저장·조회한다.
 * <ul>
 *   <li>조회: 용어(+도메인)를 읽고, 구성 단어는 IN 쿼리 한 번으로 모아 붙인다. 목록 조회도 쿼리 3개로 끝난다(내용·건수·단어).</li>
 *   <li>저장: 용어 행을 insert/update한 뒤 조인 테이블을 지우고 단어 순서대로 다시 넣는다.</li>
 * </ul>
 */
@Repository
public class TermRepository {

    private final TermMapper mapper;

    public TermRepository(TermMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. 구성 단어도 함께 저장한다. */
    public Term save(Term term) {
        if (term.getId() == null) {
            mapper.insert(term);
        } else {
            mapper.update(term);
            mapper.deleteWordsByTermId(term.getId());
        }
        List<Word> words = term.getWords();
        for (int i = 0; i < words.size(); i++) {
            mapper.insertWord(term.getId(), i + 1, words.get(i).getId());
        }
        return term;
    }

    public Optional<Term> findById(Long id) {
        return mapper.findById(id).map(term -> withWords(List.of(term)).getFirst());
    }

    /** 용어명으로 찾는다. 엑셀 업로드에서 컬럼의 용어를 이름으로 지정할 때 쓴다. */
    public Optional<Term> findByName(String name) {
        return mapper.findByName(name).map(term -> withWords(List.of(term)).getFirst());
    }

    public boolean existsByName(String name) {
        return mapper.countByName(name, null) > 0;
    }

    /** 영문약어는 단어 약어(대문자)로 조합되므로 그대로 비교한다. */
    public boolean existsByAbbreviation(String abbreviation) {
        return mapper.countByAbbreviation(abbreviation, null) > 0;
    }

    public boolean existsByNameAndIdNot(String name, Long id) {
        return mapper.countByName(name, id) > 0;
    }

    public boolean existsByAbbreviationAndIdNot(String abbreviation, Long id) {
        return mapper.countByAbbreviation(abbreviation, id) > 0;
    }

    /** 도메인을 쓰는 용어가 있는지. 도메인 삭제 시 참조 검사에 쓴다. */
    public boolean existsByDomainId(Long domainId) {
        return mapper.countByDomainId(domainId) > 0;
    }

    /** 단어를 구성 단어로 쓰는 용어가 있는지. 단어 삭제·이름 변경 시 참조 검사에 쓴다. */
    public boolean existsByWordsId(Long wordId) {
        return mapper.countByWordId(wordId) > 0;
    }

    /** 조건에 맞는 용어를 용어명 오름차순으로 페이지 조회한다. */
    public Page<Term> search(TermSearchCondition condition, Pageable pageable) {
        return Pages.of(withWords(mapper.search(condition, pageable)), pageable, () -> mapper.count(condition));
    }

    /** 조인 테이블을 먼저 지우고 용어를 지운다 (FK). */
    public void delete(Term term) {
        mapper.deleteWordsByTermId(term.getId());
        mapper.deleteById(term.getId());
    }

    /** 용어들의 구성 단어를 한 번에 읽어 채운다. */
    private List<Term> withWords(List<Term> terms) {
        if (terms.isEmpty()) {
            return terms;
        }
        List<Long> termIds = terms.stream().map(Term::getId).toList();
        Map<Long, List<Word>> wordsByTerm = InClause.query(termIds, mapper::findWordsByTermIds)
                .stream()
                .collect(groupingBy(TermWordRow::getTermId, mapping(TermWordRow::getWord, toList())));
        terms.forEach(term -> term.loadWords(wordsByTerm.getOrDefault(term.getId(), List.of())));
        return terms;
    }
}
