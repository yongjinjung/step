package com.step.dictionary.standard.word;

import com.step.dictionary.global.mybatis.InClause;
import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 표준 단어 리포지토리. 서비스가 도메인 객체 단위로 저장·조회하는 창구이고, SQL은 {@link WordMapper}가 실행한다.
 *
 * <p>MyBatis는 객체 변경을 감지하지 않는다. 엔티티를 바꾼 뒤에는 반드시 {@link #save}를 호출해야 DB에 반영된다.
 * {@code existsBy...} 메서드는 서비스에서 unique 제약 위반(500) 전에 중복을 먼저 확인해 409로 응답하는 데 쓴다.
 */
@Repository
public class WordRepository {

    private final WordMapper mapper;

    public WordRepository(WordMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록(생성된 ID가 채워진다), 있으면 수정한다. 생성·수정 일시도 채워진 같은 객체를 돌려준다. */
    public Word save(Word word) {
        if (word.getId() == null) {
            mapper.insert(word);
        } else {
            mapper.update(word);
        }
        return word;
    }

    public Optional<Word> findById(Long id) {
        return mapper.findById(id);
    }

    /** 단어명으로 찾는다. 엑셀 업로드에서 도메인의 분류어와 용어의 구성 단어를 이름으로 지정할 때 쓴다. */
    public Optional<Word> findByName(String name) {
        return mapper.findByName(name);
    }

    /** 여러 ID를 한 번에 찾는다. 순서는 보장하지 않고, 없는 ID는 결과에서 빠진다. */
    public List<Word> findAllById(Collection<Long> ids) {
        return InClause.query(ids, mapper::findAllById);
    }

    /** 등록 시 단어명 중복 확인. */
    public boolean existsByName(String name) {
        return mapper.countByName(name, null) > 0;
    }

    /** 등록 시 영문약어 중복 확인. 대문자로 정규화한 값을 넘겨야 한다. */
    public boolean existsByAbbreviation(String abbreviation) {
        return mapper.countByAbbreviation(abbreviation, null) > 0;
    }

    /** 수정 시 단어명 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByNameAndIdNot(String name, Long id) {
        return mapper.countByName(name, id) > 0;
    }

    /** 수정 시 영문약어 중복 확인. 자기 자신(id)은 제외하며 대문자로 정규화한 값을 넘겨야 한다. */
    public boolean existsByAbbreviationAndIdNot(String abbreviation, Long id) {
        return mapper.countByAbbreviation(abbreviation, id) > 0;
    }

    /**
     * 조건에 맞는 단어를 단어명 오름차순으로 페이지 조회한다.
     *
     * @param condition 검색 조건. 값이 비어 있는 조건은 무시한다
     * @param pageable  페이지 번호와 크기 (정렬 값은 쓰지 않고 단어명 순으로 고정한다)
     */
    public Page<Word> search(WordSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }

    public void delete(Word word) {
        mapper.deleteById(word.getId());
    }
}
