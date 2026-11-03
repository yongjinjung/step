package com.step.dictionary.meta.table;

import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 테이블 정의 SQL 매퍼. */
@Mapper
public interface MetaTableMapper {

    Optional<MetaTable> findById(Long id);

    long countByPhysicalName(@Param("physicalName") String physicalName, @Param("excludeId") Long excludeId);

    List<MetaTable> search(@Param("c") MetaTableSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") MetaTableSearchCondition condition);

    void insert(MetaTable table);

    void update(MetaTable table);

    void deleteById(Long id);
}
