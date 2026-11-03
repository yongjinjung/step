-- =====================================================================
-- V14: 테이블·컬럼 설명(Description)
-- ---------------------------------------------------------------------
-- 모든 테이블과 컬럼에 한글 설명을 단다. DB 도구(Workbench, DBeaver 등)와 information_schema에서 보인다.
-- 설명 문법이 DB마다 달라 같은 내용을 DB 종류별 파일로 둔다 (spring.flyway.locations의 db/vendor/{vendor}).
--   MySQL: db/vendor/mysql/V14__...sql (ALTER TABLE ... COMMENT, MODIFY COLUMN ... COMMENT)
--   H2   : db/vendor/h2/V14__...sql    (COMMENT ON TABLE / COLUMN)
-- 두 파일의 설명은 같아야 한다. 이후 테이블·컬럼을 추가하면 그 마이그레이션에서 설명도 함께 단다
-- (공통 SQL은 db/migration, 설명은 같은 버전 번호를 쓰지 않도록 새 버전으로 vendor 폴더 양쪽에 둔다).
--
-- 이 파일: MySQL 8.4. MODIFY COLUMN은 정의(타입·NULL 여부·AUTO_INCREMENT)를 바꾸지 않고 설명만 더한다.
-- =====================================================================

-- std_word
ALTER TABLE std_word
    COMMENT = '표준 단어. 용어를 구성하는 최소 단위 (예: 고객 / CUST / Customer). 분류어는 용어 끝에 붙어 데이터 성격을 나타낸다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '단어 ID (자동 증가 기본키)',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '단어명(한글). 전체에서 유일. 예) 고객',
    MODIFY COLUMN abbreviation VARCHAR(50) NOT NULL COMMENT '영문약어. 대문자로 저장하며 전체에서 유일. 용어 영문약어를 만들 때 _로 잇는다. 예) CUST',
    MODIFY COLUMN english_name VARCHAR(200) NOT NULL COMMENT '영문명(전체 표기). 유일하지 않다. 예) Customer',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN classifier CHAR(1) NOT NULL COMMENT '분류어 여부 Y/N. 분류어(번호, 명, 일자 등)만 도메인에 연결된다',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_domain
ALTER TABLE std_domain
    COMMENT = '표준 도메인. 분류어 단어별 데이터 타입·길이 규칙 (예: 번호V20 = 분류어 번호, VARCHAR(20))',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '도메인 ID (자동 증가 기본키)',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '도메인명. 전체에서 유일. 예) 번호V20',
    MODIFY COLUMN classifier_word_id BIGINT NOT NULL COMMENT '분류어 단어 ID (std_word.id). 분류어(classifier=Y) 단어만 가능',
    MODIFY COLUMN data_type VARCHAR(20) NOT NULL COMMENT '데이터 타입: CHAR, VARCHAR, DECIMAL, INTEGER, BIGINT, DATE, DATETIME, TEXT',
    MODIFY COLUMN data_length INT NULL COMMENT '길이. 길이를 쓰지 않는 타입이면 NULL',
    MODIFY COLUMN data_scale INT NULL COMMENT '소수점 자리수. DECIMAL만 값을 가지며 나머지 타입은 NULL',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_term
ALTER TABLE std_term
    COMMENT = '표준 용어. 단어를 순서대로 조합하고 도메인을 하나 연결한다 (예: 고객+번호 → 고객번호 / CUST_NO)',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '용어 ID (자동 증가 기본키)',
    MODIFY COLUMN name VARCHAR(200) NOT NULL COMMENT '용어명. 구성 단어명을 이어 붙여 만든다. 전체에서 유일. 예) 고객번호',
    MODIFY COLUMN abbreviation VARCHAR(64) NOT NULL COMMENT '영문약어(물리명). 구성 단어 약어를 _로 이어 만든다. 전체에서 유일, 최대 64자. 예) CUST_NO',
    MODIFY COLUMN domain_id BIGINT NOT NULL COMMENT '도메인 ID (std_domain.id). 컬럼 비표준 검출의 타입·길이 기준',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_term_word
ALTER TABLE std_term_word
    COMMENT = '용어 구성 단어 (용어-단어 조인 테이블). 한 행이 용어의 seq번째 단어다',
    MODIFY COLUMN term_id BIGINT NOT NULL COMMENT '용어 ID (std_term.id)',
    MODIFY COLUMN seq INT NOT NULL COMMENT '조합 순서 (1부터). 같은 단어가 여러 번 들어갈 수 있어 (term_id, seq)가 기본키다',
    MODIFY COLUMN word_id BIGINT NOT NULL COMMENT '단어 ID (std_word.id)';

-- meta_table
ALTER TABLE meta_table
    COMMENT = '테이블 정의. 실제 운영 테이블을 등록해 컬럼을 표준 용어에 매핑한다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '테이블 정의 ID (자동 증가 기본키)',
    MODIFY COLUMN physical_name VARCHAR(64) NOT NULL COMMENT '테이블 물리명. 대문자로 저장하며 전체에서 유일, 64자 이하. 예) TB_CUSTOMER',
    MODIFY COLUMN logical_name VARCHAR(200) NOT NULL COMMENT '테이블 논리명(한글명). 예) 고객',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- meta_column
ALTER TABLE meta_column
    COMMENT = '컬럼 정의. 비표준 여부는 저장하지 않고 조회할 때 현재 용어·도메인 기준으로 계산한다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '컬럼 정의 ID (자동 증가 기본키)',
    MODIFY COLUMN table_id BIGINT NOT NULL COMMENT '소속 테이블 ID (meta_table.id)',
    MODIFY COLUMN physical_name VARCHAR(64) NOT NULL COMMENT '컬럼 물리명. 대문자로 저장하며 테이블 안에서 유일. 예) CUST_NO',
    MODIFY COLUMN logical_name VARCHAR(200) NOT NULL COMMENT '컬럼 논리명(한글명). 예) 고객번호',
    MODIFY COLUMN term_id BIGINT NULL COMMENT '매핑된 표준 용어 ID (std_term.id). NULL이면 용어 미매핑(NO_TERM)으로 검출된다',
    MODIFY COLUMN data_type VARCHAR(20) NOT NULL COMMENT '데이터 타입 (도메인과 같은 타입 이름)',
    MODIFY COLUMN data_length INT NULL COMMENT '길이. 길이를 쓰지 않는 타입이면 NULL',
    MODIFY COLUMN data_scale INT NULL COMMENT '소수점 자리수. DECIMAL만 값을 가진다',
    MODIFY COLUMN nullable_yn CHAR(1) NOT NULL COMMENT 'NULL 허용 여부 Y/N',
    MODIFY COLUMN primary_key_yn CHAR(1) NOT NULL COMMENT '기본키 여부 Y/N. 기본키 컬럼은 NULL을 허용할 수 없다',
    MODIFY COLUMN seq INT NOT NULL COMMENT '테이블 안의 컬럼 순서 (1 이상, 같으면 ID 순)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)',
    MODIFY COLUMN code_group_id BIGINT NULL COMMENT '매핑된 공통 코드 그룹 ID (com_code_group.id). 선택, 비표준 검출과 무관';

-- app_user
ALTER TABLE app_user
    COMMENT = '사용자 계정. 공개 가입 없이 관리자가 만들고, 삭제하지 않고 비활성으로 둔다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '사용자 ID (자동 증가 기본키)',
    MODIFY COLUMN username VARCHAR(50) NOT NULL COMMENT '로그인 아이디. 소문자로 저장하며 전체에서 유일, 바꿀 수 없다. 예) hong.gildong',
    MODIFY COLUMN password VARCHAR(100) NOT NULL COMMENT '비밀번호 해시 (BCrypt, {bcrypt} 접두어). 평문은 저장하지 않는다',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '표시 이름. 예) 홍길동',
    MODIFY COLUMN enabled CHAR(1) NOT NULL COMMENT '활성 여부 Y/N. N이면 로그인할 수 없다',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_request
ALTER TABLE std_request
    COMMENT = '표준 데이터 변경 신청. 단어·도메인·용어의 등록·수정·삭제 신청을 검토자가 승인하면 반영된다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '신청 ID (자동 증가 기본키)',
    MODIFY COLUMN target_type VARCHAR(20) NOT NULL COMMENT '대상 종류: WORD, DOMAIN, TERM',
    MODIFY COLUMN request_type VARCHAR(20) NOT NULL COMMENT '신청 종류: CREATE, UPDATE, DELETE',
    MODIFY COLUMN target_id BIGINT NULL COMMENT '대상 ID. 수정·삭제는 필수, 등록은 승인 후 새 대상 ID가 들어간다 (대상마다 테이블이 달라 FK 없음)',
    MODIFY COLUMN target_updated_at DATETIME(6) NULL COMMENT '신청 시점 대상의 수정 일시. 승인 시 현재 값과 비교해 변경 충돌을 막는다',
    MODIFY COLUMN payload VARCHAR(4000) NULL COMMENT '신청 내용 JSON (대상별 요청 본문). 삭제 신청은 NULL',
    MODIFY COLUMN reason VARCHAR(1000) NULL COMMENT '신청 사유 (선택)',
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT '상태: REQUESTED(대기), APPROVED(승인), REJECTED(반려), CANCELED(취소)',
    MODIFY COLUMN requester_id BIGINT NOT NULL COMMENT '신청자 ID (app_user.id)',
    MODIFY COLUMN reviewer_id BIGINT NULL COMMENT '검토자 ID (app_user.id). 대기·취소 상태면 NULL',
    MODIFY COLUMN review_comment VARCHAR(1000) NULL COMMENT '검토 의견. 반려 시 필수',
    MODIFY COLUMN reviewed_at DATETIME(6) NULL COMMENT '처리 일시 (승인·반려·취소한 시각). 대기 상태면 NULL',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '신청 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- change_history
ALTER TABLE change_history
    COMMENT = '변경 이력. 표준 데이터·테이블/컬럼 정의·공통 코드가 바뀔 때마다 변경 전후 스냅샷을 남긴다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '이력 ID (자동 증가 기본키). 같은 시각의 변경도 이 값으로 순서를 구분한다',
    MODIFY COLUMN target_type VARCHAR(20) NOT NULL COMMENT '대상 종류: WORD, DOMAIN, TERM, TABLE, COLUMN, CODE_GROUP, CODE_VALUE',
    MODIFY COLUMN target_id BIGINT NOT NULL COMMENT '대상 ID (대상 테이블의 id)',
    MODIFY COLUMN change_type VARCHAR(20) NOT NULL COMMENT '변경 종류: CREATE, UPDATE, DELETE',
    MODIFY COLUMN before_data VARCHAR(4000) NULL COMMENT '변경 전 스냅샷 JSON. 등록이면 NULL',
    MODIFY COLUMN after_data VARCHAR(4000) NULL COMMENT '변경 후 스냅샷 JSON. 삭제면 NULL',
    MODIFY COLUMN changed_by BIGINT NULL COMMENT '변경자 ID (app_user.id). 신청 승인 반영이면 승인자, 시스템 작업이면 NULL',
    MODIFY COLUMN request_id BIGINT NULL COMMENT '반영한 신청 ID (std_request.id). 직접 변경이면 NULL',
    MODIFY COLUMN changed_at DATETIME(6) NOT NULL COMMENT '변경 일시';

-- com_code_group
ALTER TABLE com_code_group
    COMMENT = '공통 코드 그룹. 코드값 여러 개를 묶는 코드ID (예: ORDER_STAT 주문상태)',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '코드 그룹 ID (자동 증가 기본키). 컬럼 매핑은 이 값으로 연결한다',
    MODIFY COLUMN code_id VARCHAR(50) NOT NULL COMMENT '코드ID. 대문자로 저장하며 전체에서 유일. 예) ORDER_STAT',
    MODIFY COLUMN code_name VARCHAR(200) NOT NULL COMMENT '코드명. 예) 주문상태',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN used_yn CHAR(1) NOT NULL COMMENT '사용 여부 Y/N. 미사용 그룹은 컬럼에 새로 매핑할 수 없다',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- com_code_value
ALTER TABLE com_code_value
    COMMENT = '공통 코드값. 코드 그룹에 속한 개별 값 (예: 01 접수)',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '코드값 ID (자동 증가 기본키)',
    MODIFY COLUMN group_id BIGINT NOT NULL COMMENT '소속 코드 그룹 ID (com_code_group.id)',
    MODIFY COLUMN code_value VARCHAR(50) NOT NULL COMMENT '코드값. 대문자로 저장하며 그룹 안에서 유일. 예) 01, Y, ACTIVE',
    MODIFY COLUMN value_name VARCHAR(200) NOT NULL COMMENT '코드값명. 예) 접수',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN sort_order INT NOT NULL COMMENT '그룹 안의 정렬 순서 (1 이상, 같으면 ID 순)',
    MODIFY COLUMN used_yn CHAR(1) NOT NULL COMMENT '사용 여부 Y/N',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- app_menu
ALTER TABLE app_menu
    COMMENT = '화면 메뉴. 상위(TOP)-하위(LEFT) 2단이며, 하위 메뉴의 경로가 권한의 이름이기도 하다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '메뉴 ID (자동 증가 기본키)',
    MODIFY COLUMN parent_id BIGINT NULL COMMENT '상위 메뉴 ID (app_menu.id). NULL이면 상위(TOP) 메뉴',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '메뉴명. 예) 표준 단어',
    MODIFY COLUMN path VARCHAR(200) NULL COMMENT '화면 경로. 하위 메뉴만 가지며 전체에서 유일. 예) /words',
    MODIFY COLUMN icon VARCHAR(50) NULL COMMENT '아이콘 이름 (프론트엔드가 아이콘으로 바꾼다). 예) book',
    MODIFY COLUMN sort_order INT NOT NULL COMMENT '같은 단계 안의 정렬 순서 (같으면 ID 순)',
    MODIFY COLUMN used_yn CHAR(1) NOT NULL COMMENT '사용 여부 Y/N. 미사용 메뉴는 화면에 나오지 않는다',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- app_user_menu
ALTER TABLE app_user_menu
    COMMENT = '사용자별 메뉴 지정. 행이 있는 사용자는 지정된 메뉴만 보고, 없으면 역할 기본 규칙대로 본다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '메뉴 지정 ID (자동 증가 기본키)',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '사용자 ID (app_user.id)',
    MODIFY COLUMN menu_id BIGINT NOT NULL COMMENT '메뉴 ID (app_menu.id). 하위(LEFT) 메뉴를 지정한다',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '지정 일시 (등록 시 자동 기록). 수정하지 않고 통째로 교체한다';

-- app_role
ALTER TABLE app_role
    COMMENT = '역할. 역할별 메뉴 권한을 묶고 사용자에게 여러 개 부여한다 (권한은 합집합)',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '역할 ID (자동 증가 기본키)',
    MODIFY COLUMN code VARCHAR(50) NOT NULL COMMENT '역할 코드. 대문자로 저장하며 전체에서 유일. JWT roles 클레임에 들어간다. 예) ADMIN',
    MODIFY COLUMN name VARCHAR(100) NOT NULL COMMENT '역할명. 예) 관리자',
    MODIFY COLUMN description VARCHAR(1000) NULL COMMENT '설명 (선택)',
    MODIFY COLUMN all_permissions_yn CHAR(1) NOT NULL COMMENT '전권 여부 Y/N. Y면 메뉴 권한과 무관하게 모든 기능을 쓸 수 있다',
    MODIFY COLUMN used_yn CHAR(1) NOT NULL COMMENT '사용 여부 Y/N. 미사용 역할의 권한은 무시된다',
    MODIFY COLUMN built_in_yn CHAR(1) NOT NULL COMMENT '기본 제공 역할 여부 Y/N. Y면 삭제·코드 변경·전권 변경이 막힌다',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- app_role_menu
ALTER TABLE app_role_menu
    COMMENT = '역할별 메뉴 권한. 역할마다 메뉴의 조회·등록·수정·삭제 권한을 둔다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '역할 메뉴 권한 ID (자동 증가 기본키)',
    MODIFY COLUMN role_id BIGINT NOT NULL COMMENT '역할 ID (app_role.id)',
    MODIFY COLUMN menu_id BIGINT NOT NULL COMMENT '메뉴 ID (app_menu.id). 하위(LEFT) 메뉴에 권한을 준다',
    MODIFY COLUMN read_yn CHAR(1) NOT NULL COMMENT '조회 권한 Y/N. N이면 메뉴가 보이지 않는다',
    MODIFY COLUMN create_yn CHAR(1) NOT NULL COMMENT '등록 권한 Y/N',
    MODIFY COLUMN update_yn CHAR(1) NOT NULL COMMENT '수정 권한 Y/N',
    MODIFY COLUMN delete_yn CHAR(1) NOT NULL COMMENT '삭제 권한 Y/N',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록). 수정하지 않고 통째로 교체한다';

-- app_user_role
ALTER TABLE app_user_role
    COMMENT = '사용자 역할 (사용자-역할 조인 테이블)',
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '사용자 ID (app_user.id)',
    MODIFY COLUMN role_id BIGINT NOT NULL COMMENT '역할 ID (app_role.id)';

-- phm_prescription
ALTER TABLE phm_prescription
    COMMENT = '처방 마스터. 처방 → 조제 → 검수 → 불출 → 반납으로 이어지는 약제 업무의 시작',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '처방 ID (자동 증가 기본키)',
    MODIFY COLUMN prescription_no VARCHAR(30) NOT NULL COMMENT '처방전번호. OCS가 발급한 번호로 대문자로 저장하며 전체에서 유일',
    MODIFY COLUMN patient_no VARCHAR(20) NOT NULL COMMENT '환자번호(등록번호)',
    MODIFY COLUMN patient_name VARCHAR(100) NOT NULL COMMENT '환자명',
    MODIFY COLUMN department_code VARCHAR(20) NOT NULL COMMENT '처방 진료과 코드 (대문자). 예) IM(내과)',
    MODIFY COLUMN prescriber_name VARCHAR(100) NOT NULL COMMENT '처방의 이름',
    MODIFY COLUMN prescription_type VARCHAR(20) NOT NULL COMMENT '처방 구분: OUTPATIENT(외래), INPATIENT(입원), DISCHARGE(퇴원), EMERGENCY(응급)',
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT '처방 상태: ORDERED(처방), DISPENSED(조제됨), CANCELLED(취소). 취소는 조제 전에만 가능',
    MODIFY COLUMN prescribed_at DATETIME(6) NOT NULL COMMENT '처방 일시',
    MODIFY COLUMN remark VARCHAR(1000) NULL COMMENT '비고 (선택)',
    MODIFY COLUMN cancelled_at DATETIME(6) NULL COMMENT '취소 일시 (취소한 처방만)',
    MODIFY COLUMN cancel_reason VARCHAR(500) NULL COMMENT '취소 사유 (취소한 처방만)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_prescription_item
ALTER TABLE phm_prescription_item
    COMMENT = '처방 품목 (처방의 하위). 한 행이 처방 약품 하나다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '처방 품목 ID (자동 증가 기본키)',
    MODIFY COLUMN prescription_id BIGINT NOT NULL COMMENT '처방 ID (phm_prescription.id)',
    MODIFY COLUMN line_no INT NOT NULL COMMENT '처방 안의 순번 (1부터, 요청 순서대로)',
    MODIFY COLUMN drug_code VARCHAR(30) NOT NULL COMMENT '약품코드 (대문자). 예) AMXCLN500',
    MODIFY COLUMN drug_name VARCHAR(200) NOT NULL COMMENT '약품명. 예) 아목시실린캡슐 500mg',
    MODIFY COLUMN dose DECIMAL(12,3) NOT NULL COMMENT '1회 투여량. 예) 500',
    MODIFY COLUMN dose_unit VARCHAR(20) NOT NULL COMMENT '투여 단위 (대문자). 예) MG, ML, TAB',
    MODIFY COLUMN route VARCHAR(20) NOT NULL COMMENT '투여 경로 (대문자). 예) PO(경구), IV(정맥)',
    MODIFY COLUMN frequency VARCHAR(20) NOT NULL COMMENT '투여 빈도 (대문자). 예) TID(1일 3회)',
    MODIFY COLUMN days INT NOT NULL COMMENT '투여 일수 (1~365)',
    MODIFY COLUMN total_quantity DECIMAL(12,3) NOT NULL COMMENT '총 처방량 (조제 단위 기준). 예) 21',
    MODIFY COLUMN usage_note VARCHAR(500) NULL COMMENT '용법 설명 (선택). 예) 식후 30분';

-- phm_dispensing
ALTER TABLE phm_dispensing
    COMMENT = '조제 마스터. 처방 하나에 검수 불합격(REJECTED)이 아닌 조제는 하나뿐이다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '조제 ID (자동 증가 기본키)',
    MODIFY COLUMN prescription_id BIGINT NOT NULL COMMENT '처방 ID (phm_prescription.id)',
    MODIFY COLUMN status VARCHAR(20) NOT NULL COMMENT '조제 상태: DISPENSED(조제), INSPECTED(검수 합격), REJECTED(검수 불합격), ISSUED(불출)',
    MODIFY COLUMN dispensed_by VARCHAR(50) NOT NULL COMMENT '조제자 로그인 아이디',
    MODIFY COLUMN dispensed_at DATETIME(6) NOT NULL COMMENT '조제 일시',
    MODIFY COLUMN remark VARCHAR(1000) NULL COMMENT '비고 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_dispensing_item
ALTER TABLE phm_dispensing_item
    COMMENT = '조제 품목 (조제의 하위). 처방 품목마다 정확히 하나씩 있다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '조제 품목 ID (자동 증가 기본키)',
    MODIFY COLUMN dispensing_id BIGINT NOT NULL COMMENT '조제 ID (phm_dispensing.id)',
    MODIFY COLUMN prescription_item_id BIGINT NOT NULL COMMENT '처방 품목 ID (phm_prescription_item.id)',
    MODIFY COLUMN quantity DECIMAL(12,3) NOT NULL COMMENT '조제 수량 (0보다 크다)',
    MODIFY COLUMN lot_no VARCHAR(50) NULL COMMENT '제조번호(로트) (선택)',
    MODIFY COLUMN expiry_date DATE NULL COMMENT '유효기간 만료일 (선택)';

-- phm_inspection
ALTER TABLE phm_inspection
    COMMENT = '검수 마스터. 조제 하나에 검수 하나다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '검수 ID (자동 증가 기본키)',
    MODIFY COLUMN dispensing_id BIGINT NOT NULL COMMENT '조제 ID (phm_dispensing.id). 전체에서 유일',
    MODIFY COLUMN result VARCHAR(20) NOT NULL COMMENT '검수 결과: PASS(전 품목 합격), FAIL(불합격 품목 있음). 품목 결과로 계산한다',
    MODIFY COLUMN inspected_by VARCHAR(50) NOT NULL COMMENT '검수자 로그인 아이디',
    MODIFY COLUMN inspected_at DATETIME(6) NOT NULL COMMENT '검수 일시',
    MODIFY COLUMN remark VARCHAR(1000) NULL COMMENT '비고 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_inspection_item
ALTER TABLE phm_inspection_item
    COMMENT = '검수 품목 (검수의 하위). 조제 품목마다 정확히 하나씩 있다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '검수 품목 ID (자동 증가 기본키)',
    MODIFY COLUMN inspection_id BIGINT NOT NULL COMMENT '검수 ID (phm_inspection.id)',
    MODIFY COLUMN dispensing_item_id BIGINT NOT NULL COMMENT '조제 품목 ID (phm_dispensing_item.id)',
    MODIFY COLUMN result VARCHAR(20) NOT NULL COMMENT '품목 검수 결과: PASS, FAIL',
    MODIFY COLUMN defect_reason VARCHAR(500) NULL COMMENT '불량 사유. 불합격(FAIL)이면 필수';

-- phm_issue
ALTER TABLE phm_issue
    COMMENT = '불출 마스터. 검수 합격한 조제 하나에 불출 하나다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '불출 ID (자동 증가 기본키)',
    MODIFY COLUMN dispensing_id BIGINT NOT NULL COMMENT '조제 ID (phm_dispensing.id). 전체에서 유일',
    MODIFY COLUMN destination_code VARCHAR(20) NOT NULL COMMENT '불출처 코드 (대문자, 병동·부서·외래 창구). 예) W71',
    MODIFY COLUMN destination_name VARCHAR(100) NOT NULL COMMENT '불출처 이름. 예) 71병동',
    MODIFY COLUMN receiver_name VARCHAR(100) NOT NULL COMMENT '수령자 이름',
    MODIFY COLUMN issued_by VARCHAR(50) NOT NULL COMMENT '불출자 로그인 아이디',
    MODIFY COLUMN issued_at DATETIME(6) NOT NULL COMMENT '불출 일시',
    MODIFY COLUMN remark VARCHAR(1000) NULL COMMENT '비고 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_issue_item
ALTER TABLE phm_issue_item
    COMMENT = '불출 품목 (불출의 하위). 조제 품목을 수량 그대로 옮긴다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '불출 품목 ID (자동 증가 기본키)',
    MODIFY COLUMN issue_id BIGINT NOT NULL COMMENT '불출 ID (phm_issue.id)',
    MODIFY COLUMN dispensing_item_id BIGINT NOT NULL COMMENT '조제 품목 ID (phm_dispensing_item.id)',
    MODIFY COLUMN quantity DECIMAL(12,3) NOT NULL COMMENT '불출 수량 (= 조제 수량)';

-- phm_return
ALTER TABLE phm_return
    COMMENT = '반납 마스터. 불출 하나에 여러 번, 일부 품목만 반납할 수 있다',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반납 ID (자동 증가 기본키)',
    MODIFY COLUMN issue_id BIGINT NOT NULL COMMENT '불출 ID (phm_issue.id)',
    MODIFY COLUMN reason VARCHAR(500) NOT NULL COMMENT '반납 사유. 예) 퇴원, 처방 중단',
    MODIFY COLUMN returned_by VARCHAR(50) NOT NULL COMMENT '반납자 로그인 아이디',
    MODIFY COLUMN returned_at DATETIME(6) NOT NULL COMMENT '반납 일시',
    MODIFY COLUMN remark VARCHAR(1000) NULL COMMENT '비고 (선택)',
    MODIFY COLUMN created_at DATETIME(6) NOT NULL COMMENT '생성 일시 (등록 시 자동 기록)',
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL COMMENT '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_return_item
ALTER TABLE phm_return_item
    COMMENT = '반납 품목 (반납의 하위)',
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '반납 품목 ID (자동 증가 기본키)',
    MODIFY COLUMN return_id BIGINT NOT NULL COMMENT '반납 ID (phm_return.id)',
    MODIFY COLUMN issue_item_id BIGINT NOT NULL COMMENT '불출 품목 ID (phm_issue_item.id)',
    MODIFY COLUMN quantity DECIMAL(12,3) NOT NULL COMMENT '반납 수량 (0보다 크다). 불출 품목별 반납 합계는 불출 수량 이하',
    MODIFY COLUMN disposition VARCHAR(20) NOT NULL COMMENT '반납 후 처리: RESTOCK(재입고), DISPOSE(폐기)';
