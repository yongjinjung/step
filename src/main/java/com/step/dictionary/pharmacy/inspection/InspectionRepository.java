package com.step.dictionary.pharmacy.inspection;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.pharmacy.inspection.dto.InspectionSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 검수 리포지토리 ({@link InspectionMapper} 사용). 검수와 품목을 한 묶음으로 등록·조회한다(수정 없음). */
@Repository
public class InspectionRepository {

    private final InspectionMapper mapper;

    public InspectionRepository(InspectionMapper mapper) {
        this.mapper = mapper;
    }

    /** 검수를 품목과 함께 등록한다. */
    public Inspection save(Inspection inspection) {
        mapper.insert(inspection);
        inspection.getItems().forEach(item -> mapper.insertItem(inspection.getId(), item));
        return inspection;
    }

    /** 검수를 품목과 함께 조회한다. */
    public Optional<Inspection> findById(Long id) {
        return mapper.findById(id).map(inspection -> {
            inspection.loadItems(mapper.findItemsByInspectionId(id));
            return inspection;
        });
    }

    /** 조건에 맞는 검수를 검수 일시 최신순으로 페이지 조회한다 (품목 제외). */
    public Page<Inspection> search(InspectionSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
