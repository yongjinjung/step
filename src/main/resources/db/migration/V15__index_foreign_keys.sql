-- =====================================================================
-- V15: 외래 키 컬럼 인덱스
-- ---------------------------------------------------------------------
-- MySQL(InnoDB)은 외래 키 컬럼에 인덱스를 자동으로 만들었지만 Oracle은 만들지 않는다.
-- 인덱스가 없으면 조인·참조 검사(existsBy...)가 전체 스캔이 되고, 부모 행 삭제 시 자식 테이블 잠금 범위가 커진다.
-- 이미 앞쪽 컬럼으로 인덱스·유일 제약·기본키가 있는 외래 키는 제외했다.
--   예) com_code_value.group_id는 uk_com_code_value_group_value(group_id, code_value)가 있어 제외
--
-- 주의: 이 파일은 Oracle 21c와 테스트용 H2(MODE=Oracle) 양쪽에서 실행된다.
-- =====================================================================

-- 표준 사전
CREATE INDEX ix_std_domain_classifier_word ON std_domain (classifier_word_id);
CREATE INDEX ix_std_term_domain ON std_term (domain_id);
CREATE INDEX ix_std_term_word_word ON std_term_word (word_id);

-- 테이블·컬럼 정의
CREATE INDEX ix_meta_column_term ON meta_column (term_id);
CREATE INDEX ix_meta_column_code_group ON meta_column (code_group_id);

-- 신청·이력
CREATE INDEX ix_std_request_requester ON std_request (requester_id);
CREATE INDEX ix_std_request_reviewer ON std_request (reviewer_id);
CREATE INDEX ix_change_history_changed_by ON change_history (changed_by);
CREATE INDEX ix_change_history_request ON change_history (request_id);

-- 메뉴·권한
CREATE INDEX ix_app_user_menu_menu ON app_user_menu (menu_id);
CREATE INDEX ix_app_role_menu_menu ON app_role_menu (menu_id);
CREATE INDEX ix_app_user_role_role ON app_user_role (role_id);

-- 약제
CREATE INDEX ix_phm_dispensing_prescription ON phm_dispensing (prescription_id);
CREATE INDEX ix_phm_dispensing_item_presc_item ON phm_dispensing_item (prescription_item_id);
CREATE INDEX ix_phm_inspection_item_disp_item ON phm_inspection_item (dispensing_item_id);
CREATE INDEX ix_phm_issue_item_disp_item ON phm_issue_item (dispensing_item_id);
CREATE INDEX ix_phm_return_issue ON phm_return (issue_id);
CREATE INDEX ix_phm_return_item_issue_item ON phm_return_item (issue_item_id);
