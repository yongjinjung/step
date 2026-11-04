package com.step.dictionary.meta.column;

import com.step.dictionary.global.mybatis.InClause;
import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 컬럼 정의 리포지토리 ({@link MetaColumnMapper} 사용).
 * 조회한 컬럼에는 테이블, 용어(+도메인), 코드 그룹이 함께 채워져 있어 검출({@link MetaColumn#inspect()})과
 * 응답 변환을 추가 조회 없이 할 수 있다. 엔티티를 바꾼 뒤에는 {@link #save}를 호출한다.
 */
@Repository
public class MetaColumnRepository {

    private final MetaColumnMapper mapper;

    public MetaColumnRepository(MetaColumnMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다(코드 그룹 매핑 포함). */
    public MetaColumn save(MetaColumn column) {
        if (column.getId() == null) {
            mapper.insert(column);
        } else {
            mapper.update(column);
        }
        return column;
    }

    /** 전체 컬럼 (ID 순). 테스트에서 검출 규칙 비교에 쓴다. */
    public List<MetaColumn> findAll() {
        return mapper.findAll();
    }

    /**
     * 테이블의 전체 컬럼을 순서(seq), 같은 순서면 ID 오름차순으로 조회한다.
     * 테이블 삭제 시에도 이 메서드로 컬럼을 불러와 컬럼별 삭제 이력을 남기고 한 건씩 삭제한다.
     */
    public List<MetaColumn> findAllByTableIdOrderBySeqAscIdAsc(Long tableId) {
        return mapper.findAllByTableId(tableId);
    }

    /** 여러 테이블의 컬럼을 한 번에 조회한다. 엑셀 다운로드에서 테이블마다 따로 조회하지 않기 위해 쓴다. */
    public List<MetaColumn> findAllByTableIdInOrderBySeqAscIdAsc(Collection<Long> tableIds) {
        return InClause.query(tableIds, mapper::findAllByTableIds);
    }

    /** 코드 그룹이 매핑된 컬럼 (테이블 물리명, 순서, ID 순). 코드 그룹 상세의 매핑 컬럼 목록에 쓴다. */
    public List<MetaColumn> findAllByCodeGroupIdOrderByTablePhysicalNameAscSeqAscIdAsc(Long codeGroupId) {
        return mapper.findAllByCodeGroupId(codeGroupId);
    }

    /** 경로의 테이블에 속한 컬럼만 찾는다. 다른 테이블의 컬럼 ID로 접근하면 비어 있다. */
    public Optional<MetaColumn> findByIdAndTableId(Long id, Long tableId) {
        return mapper.findByIdAndTableId(id, tableId);
    }

    /** 등록 시 같은 테이블 안 물리명 중복 확인. 대문자로 정규화한 값을 넘긴다. */
    public boolean existsByTableIdAndPhysicalName(Long tableId, String physicalName) {
        return mapper.countByTableIdAndPhysicalName(tableId, physicalName, null) > 0;
    }

    /** 수정 시 같은 테이블 안 물리명 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByTableIdAndPhysicalNameAndIdNot(Long tableId, String physicalName, Long id) {
        return mapper.countByTableIdAndPhysicalName(tableId, physicalName, id) > 0;
    }

    /** 용어를 매핑한 컬럼이 있는지. 용어 삭제 시 참조 검사에 쓴다. */
    public boolean existsByTermId(Long termId) {
        return mapper.countByTermId(termId) > 0;
    }

    /** 코드 그룹을 매핑한 컬럼이 있는지. 코드 그룹 삭제 시 참조 검사에 쓴다. */
    public boolean existsByCodeGroupId(Long codeGroupId) {
        return mapper.countByCodeGroupId(codeGroupId) > 0;
    }

    /**
     * 비표준 컬럼을 테이블 물리명, 순서, ID 순으로 페이지 조회한다.
     * 사유를 지정하면 그 사유만, 없으면 사유가 하나라도 있는 컬럼이다.
     */
    public Page<MetaColumn> searchNonStandard(NonStandardColumnCondition condition, Pageable pageable) {
        return Pages.of(mapper.searchNonStandard(condition, pageable), pageable,
                () -> mapper.countNonStandard(condition));
    }

    public void delete(MetaColumn column) {
        mapper.deleteById(column.getId());
    }

    public void deleteAll(Collection<MetaColumn> columns) {
        columns.forEach(this::delete);
    }
}
