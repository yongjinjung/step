package com.step.dictionary.pharmacy.issue.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 불출 검색 조건. 모든 조건은 선택이다.
 *
 * @param keyword         처방전번호·환자번호·환자명 앞부분 일치 (인덱스를 타도록 중간 글자는 찾지 않는다)
 * @param destinationCode 불출처 코드 (대소문자 구분 없이 일치)
 * @param from            불출일 시작 (yyyy-MM-dd, 포함)
 * @param to              불출일 종료 (yyyy-MM-dd, 포함)
 */
public record IssueSearchCondition(
        String keyword,
        String destinationCode,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
