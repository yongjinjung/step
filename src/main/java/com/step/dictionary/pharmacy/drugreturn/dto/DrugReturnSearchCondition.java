package com.step.dictionary.pharmacy.drugreturn.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 반납 검색 조건. 모든 조건은 선택이다.
 *
 * @param keyword 처방전번호·환자번호·환자명 앞부분 일치 (인덱스를 타도록 중간 글자는 찾지 않는다)
 * @param issueId 불출 ID (그 불출의 반납만)
 * @param from    반납일 시작 (yyyy-MM-dd, 포함)
 * @param to      반납일 종료 (yyyy-MM-dd, 포함)
 */
public record DrugReturnSearchCondition(
        String keyword,
        Long issueId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
