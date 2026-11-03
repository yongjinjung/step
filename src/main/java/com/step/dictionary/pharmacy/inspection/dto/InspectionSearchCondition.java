package com.step.dictionary.pharmacy.inspection.dto;

import com.step.dictionary.pharmacy.inspection.InspectionResult;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 검수 검색 조건. 모든 조건은 선택이다.
 *
 * @param keyword 처방전번호·환자번호·환자명 앞부분 일치 (인덱스를 타도록 중간 글자는 찾지 않는다)
 * @param result  검수 결과
 * @param from    검수일 시작 (yyyy-MM-dd, 포함)
 * @param to      검수일 종료 (yyyy-MM-dd, 포함)
 */
public record InspectionSearchCondition(
        String keyword,
        InspectionResult result,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
