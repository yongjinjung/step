-- =====================================================================
-- V7: 변경 이력 (change_history)
-- ---------------------------------------------------------------------
-- 표준 단어/도메인/용어와 테이블/컬럼 정의가 바뀔 때마다 한 행씩 남긴다.
-- 변경 전후 값은 대상 엔티티의 스냅샷 JSON이다 (history.snapshot 패키지의 record).
--
--   CREATE  before_data = NULL, after_data = 등록된 값
--   UPDATE  before_data = 수정 전, after_data = 수정 후 (값이 하나도 바뀌지 않은 수정은 남기지 않음)
--   DELETE  before_data = 삭제 전, after_data = NULL
--
-- 이력은 대상 데이터를 바꾸는 서비스와 같은 트랜잭션에서 저장되므로, 변경이 실패해 롤백되면 이력도 남지 않는다.
-- 대상이 삭제돼도 이력은 남아야 하므로 target_id에는 FK를 두지 않는다 (대상 종류마다 테이블도 다르다).
-- 이력은 수정/삭제하지 않는 기록이라 created_at/updated_at 대신 changed_at 하나만 둔다.
-- =====================================================================
CREATE TABLE change_history (
    -- 이력 ID (자동 증가 기본키). 같은 시각의 변경도 이 값으로 순서를 구분한다.
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    -- 대상 종류. WORD, DOMAIN, TERM, TABLE, COLUMN
    target_type VARCHAR(20)    NOT NULL,
    -- 대상 ID (대상 테이블의 id)
    target_id   BIGINT         NOT NULL,
    -- 변경 종류. CREATE, UPDATE, DELETE
    change_type VARCHAR(20)    NOT NULL,
    -- 변경 전 스냅샷 JSON. 등록이면 NULL
    before_data VARCHAR(4000),
    -- 변경 후 스냅샷 JSON. 삭제면 NULL
    after_data  VARCHAR(4000),
    -- 변경자 (app_user.id). 신청 승인으로 반영된 변경은 승인한 관리자다.
    -- 로그인 사용자 없이 실행된 변경(시스템 작업)이면 NULL
    changed_by  BIGINT,
    -- 신청 승인으로 반영된 변경이면 그 신청 ID (std_request.id). 직접 변경이면 NULL
    request_id  BIGINT,
    -- 변경 일시
    changed_at  DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_change_history_changed_by FOREIGN KEY (changed_by) REFERENCES app_user (id),
    CONSTRAINT fk_change_history_request FOREIGN KEY (request_id) REFERENCES std_request (id)
);

-- 특정 대상의 이력 조회용
CREATE INDEX idx_change_history_target ON change_history (target_type, target_id);
-- 기간 조회용
CREATE INDEX idx_change_history_changed_at ON change_history (changed_at);
