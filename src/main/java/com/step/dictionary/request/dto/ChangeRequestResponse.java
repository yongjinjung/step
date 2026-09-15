package com.step.dictionary.request.dto;

import com.step.dictionary.request.ChangeRequest;
import com.step.dictionary.request.RequestStatus;
import com.step.dictionary.request.RequestType;
import com.step.dictionary.request.TargetType;
import com.step.dictionary.user.AppUser;
import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

/**
 * 신청 응답.
 *
 * @param id                신청 ID
 * @param targetType        대상 종류
 * @param requestType       신청 종류
 * @param targetId          대상 ID. 등록 신청은 승인 전까지 null
 * @param payload           신청 내용 (JSON 객체). 삭제 신청은 null
 * @param reason            신청 사유
 * @param status            상태
 * @param requesterUsername 신청자 아이디
 * @param requesterName     신청자 이름
 * @param reviewerUsername  검토자 아이디 (승인/반려 전이나 취소면 null)
 * @param reviewerName      검토자 이름
 * @param reviewComment     검토 의견
 * @param reviewedAt        처리 일시 (승인/반려/취소)
 * @param createdAt         신청 일시
 * @param updatedAt         최종 수정 일시
 */
public record ChangeRequestResponse(
        Long id,
        TargetType targetType,
        RequestType requestType,
        Long targetId,
        JsonNode payload,
        String reason,
        RequestStatus status,
        String requesterUsername,
        String requesterName,
        String reviewerUsername,
        String reviewerName,
        String reviewComment,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 엔티티를 응답으로 변환한다. 신청자/검토자를 읽으므로 트랜잭션 안에서 호출하거나 fetch join해 둬야 한다.
     *
     * @param payload 신청 내용 JSON을 파싱한 트리 (서비스가 매퍼로 만든다)
     */
    public static ChangeRequestResponse from(ChangeRequest request, JsonNode payload) {
        AppUser requester = request.getRequester();
        AppUser reviewer = request.getReviewer();
        return new ChangeRequestResponse(
                request.getId(),
                request.getTargetType(),
                request.getRequestType(),
                request.getTargetId(),
                payload,
                request.getReason(),
                request.getStatus(),
                requester.getUsername(),
                requester.getName(),
                reviewer == null ? null : reviewer.getUsername(),
                reviewer == null ? null : reviewer.getName(),
                request.getReviewComment(),
                request.getReviewedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}
