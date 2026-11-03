package com.step.dictionary.standard.word;

import com.step.dictionary.standard.word.dto.WordSearchCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 표준 단어 SQL 매퍼. SQL은 {@code resources/mapper/standard/word/WordMapper.xml}에 있다.
 *
 * <p>메서드 하나가 SQL 하나다. 서비스는 이 매퍼를 직접 쓰지 않고 {@link WordRepository}를 거친다.
 * 이 매퍼와 XML을 기능 패키지의 MyBatis 매퍼 기준 예시로 삼는다.
 */
@Mapper
public interface WordMapper {

    Optional<Word> findById(Long id);

    Optional<Word> findByName(String name);

    List<Word> findAllById(@Param("ids") Collection<Long> ids);

    /** 단어명 건수. {@code excludeId}가 있으면 그 단어는 세지 않는다(수정 시 자기 자신 제외). */
    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    /** 영문약어 건수. 대문자로 정규화한 값을 넘긴다. */
    long countByAbbreviation(@Param("abbreviation") String abbreviation, @Param("excludeId") Long excludeId);

    /** 조건 검색 (단어명 오름차순, LIMIT/OFFSET). */
    List<Word> search(@Param("c") WordSearchCondition condition, @Param("p") Pageable pageable);

    /** {@link #search}와 같은 조건의 전체 건수. */
    long count(@Param("c") WordSearchCondition condition);

    /** 등록. 생성된 ID가 {@code word.id}에 채워진다. */
    void insert(Word word);

    void update(Word word);

    void deleteById(Long id);
}
