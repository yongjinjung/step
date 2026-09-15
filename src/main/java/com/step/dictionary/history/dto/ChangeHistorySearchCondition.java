package com.step.dictionary.history.dto;

import com.step.dictionary.history.ChangeType;
import com.step.dictionary.history.HistoryTargetType;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 변경 이력 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param targetType 대상 종류 (null이면 전체)
 * @param targetId   대상 ID. 보통 targetType과 함께 쓴다 (종류가 다르면 ID가 겹칠 수 있다)
 * @param changeType 변경 종류 (null이면 전체)
 * @param changedBy  변경자 아이디 (대소문자 구분 없음)
 * @param requestId  신청 ID. 그 신청의 승인으로 반영된 이력만
 * @param from       시작일 (yyyy-MM-dd, 포함)
 * @param to         종료일 (yyyy-MM-dd, 포함)
 */
public record ChangeHistorySearchCondition(
        HistoryTargetType targetType,
        Long targetId,
        ChangeType changeType,
        String changedBy,
        Long requestId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
) {
}
