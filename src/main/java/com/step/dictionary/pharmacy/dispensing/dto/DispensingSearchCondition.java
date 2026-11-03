package com.step.dictionary.pharmacy.dispensing.dto;

import com.step.dictionary.pharmacy.dispensing.DispensingStatus;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 조제 검색 조건. 모든 조건은 선택이다.
 *
 * @param keyword        처방전번호·환자번호·환자명 앞부분 일치 (인덱스를 타도록 중간 글자는 찾지 않는다)
 * @param prescriptionId 처방 ID (그 처방의 조제만)
 * @param status         조제 상태
 * @param from           조제일 시작 (yyyy-MM-dd, 포함)
 * @param to             조제일 종료 (yyyy-MM-dd, 포함)
 */
public record DispensingSearchCondition(
        String keyword,
        Long prescriptionId,
        DispensingStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
