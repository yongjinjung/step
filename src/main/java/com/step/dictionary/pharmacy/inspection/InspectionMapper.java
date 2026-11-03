package com.step.dictionary.pharmacy.inspection;

import com.step.dictionary.pharmacy.inspection.dto.InspectionSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 검수 SQL 매퍼. 검수 행은 조제·처방과 함께, 품목은 조제 품목·처방 품목과 함께 읽는다. */
@Mapper
public interface InspectionMapper {

    Optional<Inspection> findById(Long id);

    /** 검수의 품목 (ID 순 = 조제 품목 순). */
    List<InspectionItem> findItemsByInspectionId(Long inspectionId);

    /** 조건 검색 (검수 일시 최신순, 같으면 ID 내림차순). 품목은 읽지 않는다. */
    List<Inspection> search(@Param("c") InspectionSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") InspectionSearchCondition condition);

    void insert(Inspection inspection);

    void insertItem(@Param("inspectionId") Long inspectionId, @Param("item") InspectionItem item);
}
