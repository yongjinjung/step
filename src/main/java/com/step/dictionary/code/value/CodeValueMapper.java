package com.step.dictionary.code.value;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 공통 코드값 SQL 매퍼. 조회 결과에 소속 코드 그룹({@code group})을 함께 채운다. */
@Mapper
public interface CodeValueMapper {

    /** 그룹의 코드값을 정렬 순서, 같으면 ID 오름차순으로. */
    List<CodeValue> findAllByGroupId(Long groupId);

    Optional<CodeValue> findByIdAndGroupId(@Param("id") Long id, @Param("groupId") Long groupId);

    long countByGroupIdAndCodeValue(@Param("groupId") Long groupId, @Param("codeValue") String codeValue,
            @Param("excludeId") Long excludeId);

    void insert(CodeValue value);

    void update(CodeValue value);

    void deleteById(Long id);
}
