package com.step.dictionary.code.group;

import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 공통 코드 그룹 SQL 매퍼. */
@Mapper
public interface CodeGroupMapper {

    Optional<CodeGroup> findById(Long id);

    long countByCodeId(@Param("codeId") String codeId, @Param("excludeId") Long excludeId);

    List<CodeGroup> search(@Param("c") CodeGroupSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") CodeGroupSearchCondition condition);

    void insert(CodeGroup group);

    void update(CodeGroup group);

    void deleteById(Long id);
}
