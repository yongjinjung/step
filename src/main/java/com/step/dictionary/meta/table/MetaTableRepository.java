package com.step.dictionary.meta.table;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 테이블 정의 리포지토리 ({@link MetaTableMapper} 사용). 엔티티를 바꾼 뒤에는 {@link #save}를 호출한다. */
@Repository
public class MetaTableRepository {

    private final MetaTableMapper mapper;

    public MetaTableRepository(MetaTableMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. */
    public MetaTable save(MetaTable table) {
        if (table.getId() == null) {
            mapper.insert(table);
        } else {
            mapper.update(table);
        }
        return table;
    }

    public Optional<MetaTable> findById(Long id) {
        return mapper.findById(id);
    }

    /** 등록 시 물리명 중복 확인. 대문자로 정규화한 값을 넘긴다. */
    public boolean existsByPhysicalName(String physicalName) {
        return mapper.countByPhysicalName(physicalName, null) > 0;
    }

    /** 수정 시 물리명 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByPhysicalNameAndIdNot(String physicalName, Long id) {
        return mapper.countByPhysicalName(physicalName, id) > 0;
    }

    /** 조건에 맞는 테이블을 물리명 오름차순으로 페이지 조회한다. */
    public Page<MetaTable> search(MetaTableSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }

    /** 테이블만 지운다. 컬럼은 서비스가 먼저 지워야 한다 (FK). */
    public void delete(MetaTable table) {
        mapper.deleteById(table.getId());
    }
}
