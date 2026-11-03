package com.step.dictionary.pharmacy.dispensing;

import com.step.dictionary.pharmacy.dispensing.dto.DispensingSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 조제 SQL 매퍼. 조제 행은 처방(행)과 함께, 품목은 처방 품목과 함께 읽는다.
 * XML에는 검수·불출 매퍼가 쓰는 조제·조제 품목 결과 매핑과 컬럼 조각도 있다.
 */
@Mapper
public interface DispensingMapper {

    Optional<Dispensing> findById(Long id);

    /** 조제의 품목 (ID 순 = 처방 순번 순). */
    List<DispensingItem> findItemsByDispensingId(Long dispensingId);

    /** 처방의 조제 중 지정한 상태가 아닌 것의 수. */
    long countByPrescriptionIdAndStatusNot(@Param("prescriptionId") Long prescriptionId,
            @Param("status") DispensingStatus status);

    /** 조건 검색 (조제 일시 최신순, 같으면 ID 내림차순). 품목은 읽지 않는다. */
    List<Dispensing> search(@Param("c") DispensingSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") DispensingSearchCondition condition);

    void insert(Dispensing dispensing);

    void insertItem(@Param("dispensingId") Long dispensingId, @Param("item") DispensingItem item);

    /** 상태만 바뀐다 (검수 결과 반영, 불출). */
    void updateStatus(Dispensing dispensing);
}
