package com.step.dictionary.meta.column;

import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 컬럼 정의 SQL 매퍼. 조회 결과에는 테이블, 용어(+도메인, 분류어), 코드 그룹이 함께 채워진다.
 * 용어의 구성 단어는 채우지 않는다(컬럼 화면과 검출에는 필요 없다).
 *
 * <p>비표준 검색 조건(XML의 {@code issueCondition})은 {@link MetaColumn#inspect()}와 같은 규칙이어야 한다.
 * 규칙을 바꾸면 둘 다 고치고 {@code MetaColumnRepositoryTest.searchAgreesWithInspect}로 일치를 확인한다.
 */
@Mapper
public interface MetaColumnMapper {

    List<MetaColumn> findAll();

    /** 테이블의 컬럼을 순서(seq), 같으면 ID 오름차순으로. */
    List<MetaColumn> findAllByTableId(Long tableId);

    /** 여러 테이블의 컬럼을 테이블 상관없이 순서(seq), 같으면 ID 오름차순으로. */
    List<MetaColumn> findAllByTableIds(@Param("tableIds") Collection<Long> tableIds);

    /** 코드 그룹이 매핑된 컬럼을 테이블 물리명, 순서, ID 오름차순으로. */
    List<MetaColumn> findAllByCodeGroupId(Long codeGroupId);

    Optional<MetaColumn> findByIdAndTableId(@Param("id") Long id, @Param("tableId") Long tableId);

    long countByTableIdAndPhysicalName(@Param("tableId") Long tableId, @Param("physicalName") String physicalName,
            @Param("excludeId") Long excludeId);

    long countByTermId(Long termId);

    long countByCodeGroupId(Long codeGroupId);

    List<MetaColumn> searchNonStandard(@Param("c") NonStandardColumnCondition condition,
            @Param("p") Pageable pageable);

    long countNonStandard(@Param("c") NonStandardColumnCondition condition);

    void insert(MetaColumn column);

    void update(MetaColumn column);

    void deleteById(Long id);
}
