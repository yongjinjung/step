-- =====================================================================
-- V6: 표준 데이터 변경 신청 (std_request)
-- ---------------------------------------------------------------------
-- 사용자가 표준 단어/도메인/용어의 등록·수정·삭제를 신청하고 관리자가 승인하면 반영한다.
-- 승인할 때 기존 서비스(WordService 등)를 그대로 호출하므로 모든 업무 규칙이 같은 방식으로 적용된다.
--
-- 상태 흐름:
--   REQUESTED ─┬─ 승인(ADMIN)   → APPROVED  (같은 트랜잭션에서 대상에 반영)
--              ├─ 반려(ADMIN)   → REJECTED
--              └─ 취소(신청자)  → CANCELED
--   REQUESTED가 아닌 신청은 다시 처리할 수 없다.
--
-- 규칙:
--   - 수정/삭제 신청은 신청 시점 대상의 updated_at을 target_updated_at에 기록하고,
--     승인 시점의 값과 다르면(그사이 대상이 바뀌거나 삭제됨) 승인을 거부한다.
--   - 같은 대상(target_type, target_id)에 REQUESTED 상태 신청은 하나만 둔다.
--
-- 이 파일은 MySQL 8.4와 H2(MODE=MySQL) 양쪽에서 실행된다. TEXT 대신 VARCHAR를 쓰는 이유는
-- 두 DB가 TEXT를 서로 다른 JDBC 타입으로 보고해 ddl-auto=validate가 어긋나기 때문이다.
-- =====================================================================
CREATE TABLE std_request (
    -- 신청 ID (자동 증가 기본키)
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    -- 대상 종류. WORD, DOMAIN, TERM
    target_type       VARCHAR(20)    NOT NULL,
    -- 신청 종류. CREATE, UPDATE, DELETE
    request_type      VARCHAR(20)    NOT NULL,
    -- 대상 ID. 수정/삭제는 신청 시 필수. 등록은 신청 시 NULL이고 승인되면 새로 만들어진 대상의 ID가 들어간다.
    -- 대상 종류마다 참조 테이블이 달라 FK를 두지 않는다.
    target_id         BIGINT,
    -- 수정/삭제 신청 시점 대상의 updated_at. 승인 시 현재 값과 비교해 변경 충돌을 막는다. 등록 신청은 NULL.
    target_updated_at DATETIME(6),
    -- 신청 내용 JSON. 대상별 요청 본문(WordRequest, DomainRequest, TermRequest)을 그대로 직렬화한다. 삭제 신청은 NULL.
    payload           VARCHAR(4000),
    -- 신청 사유 (선택)
    reason            VARCHAR(1000),
    -- 상태. REQUESTED, APPROVED, REJECTED, CANCELED
    status            VARCHAR(20)    NOT NULL,
    -- 신청자 (app_user.id)
    requester_id      BIGINT         NOT NULL,
    -- 검토자 (app_user.id). 승인/반려한 관리자. 대기·취소 상태면 NULL
    reviewer_id       BIGINT,
    -- 검토 의견. 반려 시 필수, 승인 시 선택
    review_comment    VARCHAR(1000),
    -- 처리 일시 (승인/반려/취소한 시각). 대기 상태면 NULL
    reviewed_at       DATETIME(6),
    -- 신청 일시 (JPA 감사)
    created_at        DATETIME(6)    NOT NULL,
    -- 최종 수정 일시 (JPA 감사)
    updated_at        DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_std_request_requester FOREIGN KEY (requester_id) REFERENCES app_user (id),
    CONSTRAINT fk_std_request_reviewer FOREIGN KEY (reviewer_id) REFERENCES app_user (id)
);

-- 같은 대상의 대기 중 신청 존재 여부 확인용 (신청 시 중복 검사)
CREATE INDEX idx_std_request_target ON std_request (target_type, target_id, status);
