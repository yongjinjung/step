package com.step.dictionary.pharmacy.prescription;

import com.step.dictionary.pharmacy.prescription.dto.PrescriptionSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 처방 SQL 매퍼. 처방 행과 품목을 따로 읽고 {@link PrescriptionRepository}가 묶는다.
 * XML에는 다른 약제 매퍼(조제·검수·불출·반납)가 쓰는 처방·처방 품목 컬럼 조각과 검색어 조건도 있다.
 */
@Mapper
public interface PrescriptionMapper {

    Optional<Prescription> findById(Long id);

    /** 처방의 품목 (순번 순). */
    List<PrescriptionItem> findItemsByPrescriptionId(Long prescriptionId);

    long countByPrescriptionNo(String prescriptionNo);

    /** 조건 검색 (처방 일시 최신순, 같으면 ID 내림차순). 품목은 읽지 않는다. */
    List<Prescription> search(@Param("c") PrescriptionSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") PrescriptionSearchCondition condition);

    void insert(Prescription prescription);

    void insertItem(@Param("prescriptionId") Long prescriptionId, @Param("item") PrescriptionItem item);

    /** 상태와 취소 정보만 바뀐다 (처방 내용은 수정하지 않는다). */
    void updateStatus(Prescription prescription);
}
