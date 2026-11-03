package com.step.dictionary.history.dto;

import com.step.dictionary.history.ChangeHistory;
import com.step.dictionary.history.ChangeType;
import com.step.dictionary.history.HistoryTargetType;
import com.step.dictionary.user.AppUser;
import java.time.LocalDateTime;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * 변경 이력 응답.
 *
 * @param id                이력 ID
 * @param targetType        대상 종류
 * @param targetId          대상 ID
 * @param changeType        변경 종류
 * @param before            변경 전 스냅샷 (JSON 객체). 등록이면 null
 * @param after             변경 후 스냅샷 (JSON 객체). 삭제면 null
 * @param changedFields     달라진 필드 이름 ({@code ChangedFields} 규칙)
 * @param changedByUsername 변경자 아이디 (시스템 작업이면 null)
 * @param changedByName     변경자 이름
 * @param requestId         신청 승인으로 반영됐으면 신청 ID
 * @param changedAt         변경 일시
 */
public record ChangeHistoryResponse(
        Long id,
        HistoryTargetType targetType,
        Long targetId,
        ChangeType changeType,
        JsonNode before,
        JsonNode after,
        List<String> changedFields,
        String changedByUsername,
        String changedByName,
        Long requestId,
        LocalDateTime changedAt
) {

    /** 엔티티와 서비스가 계산한 값으로 응답을 만든다. 변경자는 조회 쿼리에서 조인해 채워 둔다. */
    public static ChangeHistoryResponse from(ChangeHistory history, JsonNode before, JsonNode after,
            List<String> changedFields) {
        AppUser changedBy = history.getChangedBy();
        return new ChangeHistoryResponse(
                history.getId(),
                history.getTargetType(),
                history.getTargetId(),
                history.getChangeType(),
                before,
                after,
                changedFields,
                changedBy == null ? null : changedBy.getUsername(),
                changedBy == null ? null : changedBy.getName(),
                history.getRequestId(),
                history.getChangedAt());
    }
}
