-- =====================================================================
-- 공통 코드 예시 데이터 (코드 그룹 19개 / 코드값 100개)
-- ---------------------------------------------------------------------
-- 업무에서 흔히 쓰는 일반적인 공통 코드다. 개발·시연용 샘플이며 업무 표준이 아니므로
-- 실제 프로젝트에서는 필요한 그룹만 골라 쓰거나 값을 바꿔서 쓴다.
--
-- Flyway 마이그레이션이 아니다. Flyway는 `db/migration`만 읽으므로 이 파일은 자동 실행되지 않는다.
-- (자동 실행되면 테스트용 H2에도 들어가 검색 결과를 기대하는 테스트가 깨진다.)
--
-- 넣는 방법 (로컬 Oracle, Git Bash). NLS_LANG으로 UTF-8을 지정해야 한글이 깨지지 않는다:
--   docker exec -i -e NLS_LANG=AMERICAN_AMERICA.AL32UTF8 pharmacy-oracle sqlplus -s pharmacy/pharmacy@XEPDB1 \
--     < src/main/resources/db/seed/common_code_seed.sql
--
-- 되돌리는 방법 (아래 그룹의 코드값 → 그룹 순서로 지운다):
--   DELETE FROM com_code_value WHERE group_id IN
--     (SELECT id FROM com_code_group WHERE code_id IN ('USE_YN','GENDER',...));
--   DELETE FROM com_code_group WHERE code_id IN ('USE_YN','GENDER',...);
--   단, 컬럼에 매핑된 그룹은 FK 때문에 지울 수 없다 (매핑을 먼저 해제한다).
--
-- 주의:
--   - 한 번만 실행한다. 다시 실행하면 코드ID 유일 제약(uk_com_code_group_code_id)에 걸린다.
--   - SQL로 직접 넣으므로 변경 이력(change_history)은 남지 않는다. 화면·API로 넣으면 이력이 남는다.
--   - 코드ID와 코드값은 애플리케이션이 대문자로 저장하므로 여기서도 대문자로 넣는다.
-- =====================================================================

-- sqlplus 설정: & 치환 끄기, 문장 안 빈 줄 허용, 오류 시 롤백하고 종료.
SET DEFINE OFF
SET SQLBLANKLINES ON
SET FEEDBACK OFF
WHENEVER SQLERROR EXIT FAILURE ROLLBACK

-- ---------------------------------------------------------------------
-- 1. 코드 그룹 (19건)
-- ---------------------------------------------------------------------

INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('USE_YN',      '사용여부',     '사용/미사용 여부',                   'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('GENDER',      '성별',         '회원·고객 성별',                     'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('MBR_GRAD',    '회원등급',     '구매 실적 기준 회원 등급',           'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('MBR_STAT',    '회원상태',     '회원 계정 상태',                     'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('ORDER_STAT',  '주문상태',     '주문 처리 단계',                     'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('PAY_TYPE',    '결제수단',     '주문 결제 수단',                     'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('PAY_STAT',    '결제상태',     '결제 처리 상태',                     'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('DLVR_STAT',   '배송상태',     '배송 진행 상태',                     'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('RTN_RSN',     '반품사유',     '반품·교환 신청 사유',                'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('BANK_CD',     '은행',         '금융결제원 은행 코드 일부',          'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('CARD_CD',     '카드사',       '신용카드 발급사',                    'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('CURR_CD',     '통화',         'ISO 4217 통화 코드 일부',            'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('NATION_CD',   '국가',         'ISO 3166-1 alpha-2 국가 코드 일부',  'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('TAX_TYPE',    '과세구분',     '세금계산서 과세 구분',               'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('BIZ_TYPE',    '사업자구분',   '거래처 사업자 구분',                 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('APRV_STAT',   '결재상태',     '전자결재 문서 상태',                 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('EMP_STAT',    '재직상태',     '임직원 재직 상태',                   'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('NOTI_TYPE',   '알림유형',     '고객 알림 발송 수단',                'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_group (code_id, code_name, description, used_yn, created_at, updated_at) VALUES ('DEV_TYPE',    '접속기기',     '서비스 접속 기기·채널',              'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- ---------------------------------------------------------------------
-- 2. 코드값 (100건)
--    그룹 ID는 코드ID로 찾아 변수에 담아 쓴다 (AUTO_INCREMENT 값을 가정하지 않기 위해서다).
-- ---------------------------------------------------------------------

-- 사용여부 (2건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'USE_YN'), 'Y', '사용',   NULL, 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'USE_YN'), 'N', '미사용', NULL, 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 성별 (3건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'GENDER'), 'M', '남성', NULL,                  1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'GENDER'), 'F', '여성', NULL,                  2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'GENDER'), 'U', '미상', '미입력 또는 확인 불가', 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 회원등급 (5건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_GRAD'), '10', '일반',     '가입 시 기본 등급', 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_GRAD'), '20', '실버',     NULL,               2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_GRAD'), '30', '골드',     NULL,               3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_GRAD'), '40', '플래티넘', NULL,               4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_GRAD'), '50', 'VIP',      '최상위 등급',       5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 회원상태 (4건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_STAT'), '10', '정상', NULL,                      1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_STAT'), '20', '휴면', '장기 미접속으로 전환',     2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_STAT'), '30', '정지', '이용 약관 위반 등으로 정지', 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'MBR_STAT'), '40', '탈퇴', NULL,                      4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 주문상태 (9건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '10', '주문접수',   '주문서 생성, 결제 전', 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '20', '결제완료',   NULL,                  2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '30', '상품준비중', NULL,                  3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '40', '배송중',     NULL,                  4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '50', '배송완료',   NULL,                  5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '60', '구매확정',   '반품·교환 기간 종료',  6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '70', '주문취소',   NULL,                  7, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '80', '반품',       NULL,                  8, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'ORDER_STAT'), '90', '교환',       NULL,                  9, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 결제수단 (8건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '10', '신용카드',   NULL,                     1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '20', '체크카드',   NULL,                     2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '30', '계좌이체',   '실시간 계좌이체',         3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '40', '가상계좌',   '무통장 입금',             4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '50', '휴대폰결제', NULL,                     5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '60', '간편결제',   '카카오페이·네이버페이 등', 6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '70', '포인트',     '적립금 사용',             7, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_TYPE'), '80', '상품권',     NULL,                     8, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 결제상태 (6건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_STAT'), '10', '결제대기', '입금 전 상태',       1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_STAT'), '20', '결제완료', NULL,                2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_STAT'), '30', '결제취소', '전체 취소',          3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_STAT'), '40', '부분취소', '일부 금액만 취소',    4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_STAT'), '50', '환불완료', NULL,                5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'PAY_STAT'), '60', '결제실패', '승인 거절·한도 초과', 6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 배송상태 (5건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DLVR_STAT'), '10', '배송준비', NULL,                   1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DLVR_STAT'), '20', '집화완료', '택배사 인수',          2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DLVR_STAT'), '30', '배송중',   NULL,                   3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DLVR_STAT'), '40', '배송완료', NULL,                   4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DLVR_STAT'), '50', '배송보류', '주소 불명·수취인 부재', 5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 반품사유 (5건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'RTN_RSN'), '10', '단순변심', '고객 귀책',   1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'RTN_RSN'), '20', '상품불량', '판매자 귀책', 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'RTN_RSN'), '30', '오배송',   '판매자 귀책', 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'RTN_RSN'), '40', '배송지연', NULL,          4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'RTN_RSN'), '50', '기타',     NULL,          5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 은행 (8건). 코드값은 금융결제원 은행 코드다.
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '003', 'IBK기업은행',  NULL, 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '004', 'KB국민은행',   NULL, 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '011', 'NH농협은행',   NULL, 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '020', '우리은행',     NULL, 4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '081', '하나은행',     NULL, 5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '088', '신한은행',     NULL, 6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '090', '카카오뱅크',   NULL, 7, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BANK_CD'), '092', '토스뱅크',     NULL, 8, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 카드사 (8건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '01', '비씨카드',   NULL, 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '02', 'KB국민카드', NULL, 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '03', '하나카드',   NULL, 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '04', '삼성카드',   NULL, 4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '05', '신한카드',   NULL, 5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '06', '현대카드',   NULL, 6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '07', '롯데카드',   NULL, 7, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CARD_CD'), '08', 'NH농협카드', NULL, 8, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 통화 (5건). 코드값은 ISO 4217이다.
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CURR_CD'), 'KRW', '대한민국 원', NULL, 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CURR_CD'), 'USD', '미국 달러',   NULL, 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CURR_CD'), 'EUR', '유로',        NULL, 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CURR_CD'), 'JPY', '일본 엔',     NULL, 4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'CURR_CD'), 'CNY', '중국 위안',   NULL, 5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 국가 (8건). 코드값은 ISO 3166-1 alpha-2다.
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'KR', '대한민국', NULL, 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'US', '미국',     NULL, 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'JP', '일본',     NULL, 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'CN', '중국',     NULL, 4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'VN', '베트남',   NULL, 5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'GB', '영국',     NULL, 6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'DE', '독일',     NULL, 7, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NATION_CD'), 'FR', '프랑스',   NULL, 8, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 과세구분 (3건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'TAX_TYPE'), '10', '과세', '부가세 10%',    1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'TAX_TYPE'), '20', '면세', '부가세 면제',   2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'TAX_TYPE'), '30', '영세', '수출 등 영세율', 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 사업자구분 (3건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BIZ_TYPE'), '10', '개인사업자', NULL,              1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BIZ_TYPE'), '20', '법인사업자', NULL,              2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'BIZ_TYPE'), '30', '비사업자',   '개인 고객 등',     3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 결재상태 (6건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'APRV_STAT'), '10', '임시저장', NULL,                  1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'APRV_STAT'), '20', '상신',     '결재 요청',           2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'APRV_STAT'), '30', '진행중',   '결재선 처리 중',      3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'APRV_STAT'), '40', '승인',     NULL,                  4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'APRV_STAT'), '50', '반려',     NULL,                  5, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'APRV_STAT'), '60', '회수',     '기안자가 상신 취소',   6, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 재직상태 (4건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'EMP_STAT'), '10', '재직', NULL,              1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'EMP_STAT'), '20', '휴직', NULL,              2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'EMP_STAT'), '30', '파견', '타 조직 파견 중',  3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'EMP_STAT'), '40', '퇴직', NULL,              4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 알림유형 (4건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NOTI_TYPE'), '10', '이메일', NULL,               1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NOTI_TYPE'), '20', 'SMS',    '단문·장문 문자',    2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NOTI_TYPE'), '30', '앱푸시', NULL,               3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'NOTI_TYPE'), '40', '알림톡', '카카오 알림톡',     4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
-- 접속기기 (4건)
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DEV_TYPE'), '10', 'PC웹',       NULL, 1, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DEV_TYPE'), '20', '모바일웹',   NULL, 2, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DEV_TYPE'), '30', '안드로이드앱', NULL, 3, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);
INSERT INTO com_code_value (group_id, code_value, value_name, description, sort_order, used_yn, created_at, updated_at) VALUES ((SELECT id FROM com_code_group WHERE code_id = 'DEV_TYPE'), '40', 'iOS앱',      NULL, 4, 'Y', LOCALTIMESTAMP, LOCALTIMESTAMP);

EXIT
