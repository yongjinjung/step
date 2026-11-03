package com.step.dictionary.standard.domain;

import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 표준 도메인 SQL 매퍼. 조회 결과에 분류어 단어({@code classifierWord})를 함께 채운다. */
@Mapper
public interface DomainMapper {

    Optional<Domain> findById(Long id);

    Optional<Domain> findByName(String name);

    long countByName(@Param("name") String name, @Param("excludeId") Long excludeId);

    long countByClassifierWordId(Long wordId);

    List<Domain> search(@Param("c") DomainSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") DomainSearchCondition condition);

    void insert(Domain domain);

    void update(Domain domain);

    void deleteById(Long id);
}
