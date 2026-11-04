package com.step.dictionary.request;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.user.AppUser;
import java.time.LocalDateTime;

/**
 * 표준 데이터 변경 신청.
 *
 * <p>테이블: {@code std_request} (V6 마이그레이션). 사용자가 단어/도메인/용어의 등록·수정·삭제를 신청하고,
 * 관리자가 승인하면 {@link StandardDataApplier}가 기존 서비스를 호출해 반영한다.
 *
 * <p>상태 전이 메서드({@link #approve}, {@link #reject}, {@link #cancel})는 상태만 바꾼다.
 * "대기 중일 때만 처리 가능", "본인만 취소" 같은 검사는 {@link ChangeRequestService}에서 먼저 한다.
 */
public class ChangeRequest extends BaseTimeEntity {

    /** 신청 ID (AUTO_INCREMENT). */
    private Long id;
    /** 대상 종류. */
    private TargetType targetType;

    /** 신청 종류. */
    private RequestType requestType;

    /** 대상 ID. 등록 신청은 승인 전까지 null이고, 승인되면 새 대상의 ID가 들어간다. */
    private Long targetId;

    /** 수정/삭제 신청 시점 대상의 수정 일시. 승인 시 현재 값과 비교해 충돌을 막는다. 등록 신청은 null. */
    private LocalDateTime targetUpdatedAt;

    /** 신청 내용 JSON (WordRequest/DomainRequest/TermRequest 직렬화). 삭제 신청은 null. */
    private String payload;

    /** 신청 사유 (선택). */
    private String reason;

    /** 상태. */
    private RequestStatus status;

    /** 신청자. */
    private AppUser requester;

    /** 검토자 (승인/반려한 관리자). 대기·취소 상태면 null. */
    private AppUser reviewer;

    /** 검토 의견. 반려 시 필수, 승인 시 선택. */
    private String reviewComment;

    /** 처리 일시 (승인/반려/취소 시각). */
    private LocalDateTime reviewedAt;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected ChangeRequest() {
    }

    /**
     * 새 신청을 만든다. 처리 대기 상태로 시작한다.
     *
     * @param targetType      대상 종류
     * @param requestType     신청 종류
     * @param targetId        대상 ID (등록 신청은 null)
     * @param targetUpdatedAt 신청 시점 대상의 수정 일시 (등록 신청은 null)
     * @param payload         신청 내용 JSON (삭제 신청은 null)
     * @param reason          신청 사유 (null 가능)
     * @param requester       신청자
     */
    public ChangeRequest(TargetType targetType, RequestType requestType, Long targetId, LocalDateTime targetUpdatedAt,
            String payload, String reason, AppUser requester) {
        this.targetType = targetType;
        this.requestType = requestType;
        this.targetId = targetId;
        this.targetUpdatedAt = targetUpdatedAt;
        this.payload = payload;
        this.reason = reason;
        this.requester = requester;
        this.status = RequestStatus.REQUESTED;
    }

    /**
     * 승인 처리. 대상 반영이 성공한 뒤에 호출한다.
     *
     * @param reviewer        승인한 관리자
     * @param comment         의견 (null 가능)
     * @param reviewedAt      처리 시각
     * @param appliedTargetId 반영된 대상 ID. 등록 신청은 새로 만들어진 ID, 나머지는 기존 ID
     */
    public void approve(AppUser reviewer, String comment, LocalDateTime reviewedAt, Long appliedTargetId) {
        this.status = RequestStatus.APPROVED;
        this.reviewer = reviewer;
        this.reviewComment = comment;
        this.reviewedAt = reviewedAt;
        this.targetId = appliedTargetId;
    }

    /** 반려 처리. 대상은 바꾸지 않는다. */
    public void reject(AppUser reviewer, String comment, LocalDateTime reviewedAt) {
        this.status = RequestStatus.REJECTED;
        this.reviewer = reviewer;
        this.reviewComment = comment;
        this.reviewedAt = reviewedAt;
    }

    /** 신청자의 취소 처리. 검토자는 비워 둔다. */
    public void cancel(LocalDateTime canceledAt) {
        this.status = RequestStatus.CANCELED;
        this.reviewedAt = canceledAt;
    }

    /** 처리 대기 중인지. 승인·반려·취소는 대기 중일 때만 할 수 있다. */
    public boolean isPending() {
        return status == RequestStatus.REQUESTED;
    }

    /** 이 사용자가 신청했는지. */
    public boolean isRequestedBy(String username) {
        return requester.getUsername().equals(username);
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public LocalDateTime getTargetUpdatedAt() {
        return targetUpdatedAt;
    }

    public String getPayload() {
        return payload;
    }

    public String getReason() {
        return reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public AppUser getRequester() {
        return requester;
    }

    public AppUser getReviewer() {
        return reviewer;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
}
