package com.step.dictionary.history;

import com.step.dictionary.user.AppUser;
import java.time.LocalDateTime;

/**
 * 변경 이력 한 건.
 *
 * <p>테이블: {@code change_history} (V7 마이그레이션). {@link ChangeHistoryRecorder}만 만들고, 만든 뒤에는 바꾸지 않는다.
 * 그래서 setter도, 수정 메서드도 없고 {@code BaseTimeEntity}를 상속하지 않는다(변경 일시 하나로 충분하다).
 */
public class ChangeHistory {

    /** 이력 ID (AUTO_INCREMENT). */
    private Long id;

    // enum 컬럼은 MySQL 방언이 ENUM 타입으로 보지 않도록 VARCHAR로 지정한다 (이하 동일).
    /** 대상 종류. */
    private HistoryTargetType targetType;

    /** 대상 ID. 대상이 삭제돼도 남도록 FK 연관 없이 값만 둔다. */
    private Long targetId;

    /** 변경 종류. */
    private ChangeType changeType;

    /** 변경 전 스냅샷 JSON. 등록이면 null. */
    private String beforeData;

    /** 변경 후 스냅샷 JSON. 삭제면 null. */
    private String afterData;

    /** 변경자. 로그인 사용자 없이 실행된 변경이면 null. */
    private AppUser changedBy;

    /** 신청 승인으로 반영된 변경이면 신청 ID. 신청 엔티티를 쓸 일이 없어 연관 없이 값만 둔다(DB에는 FK가 있다). */
    private Long requestId;

    /** 변경 일시. */
    private LocalDateTime changedAt;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected ChangeHistory() {
    }

    /**
     * 이력을 만든다.
     *
     * @param targetType 대상 종류
     * @param targetId   대상 ID
     * @param changeType 변경 종류
     * @param beforeData 변경 전 JSON (등록이면 null)
     * @param afterData  변경 후 JSON (삭제면 null)
     * @param changedBy  변경자 (null 가능)
     * @param requestId  신청 ID (직접 변경이면 null)
     * @param changedAt  변경 일시
     */
    public ChangeHistory(HistoryTargetType targetType, Long targetId, ChangeType changeType, String beforeData,
            String afterData, AppUser changedBy, Long requestId, LocalDateTime changedAt) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.changeType = changeType;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.changedBy = changedBy;
        this.requestId = requestId;
        this.changedAt = changedAt;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public HistoryTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getBeforeData() {
        return beforeData;
    }

    public String getAfterData() {
        return afterData;
    }

    public AppUser getChangedBy() {
        return changedBy;
    }

    public Long getRequestId() {
        return requestId;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
