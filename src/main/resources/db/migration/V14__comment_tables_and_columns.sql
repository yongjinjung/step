-- =====================================================================
-- V14: 테이블·컬럼 설명(Description)
-- ---------------------------------------------------------------------
-- 모든 테이블과 컬럼에 한글 설명을 단다. DB 도구(SQL Developer, DBeaver 등)와 데이터 사전
-- (USER_TAB_COMMENTS, USER_COL_COMMENTS)에서 보인다. COMMENT ON 문법은 Oracle과 테스트용 H2가 같다.
-- 이후 테이블·컬럼을 추가하면 그 마이그레이션에서 COMMENT ON으로 설명도 함께 단다 (SchemaCommentTest가 검사).
-- =====================================================================

-- std_word
COMMENT ON TABLE std_word IS '표준 단어. 용어를 구성하는 최소 단위 (예: 고객 / CUST / Customer). 분류어는 용어 끝에 붙어 데이터 성격을 나타낸다';
COMMENT ON COLUMN std_word.id IS '단어 ID (자동 증가 기본키)';
COMMENT ON COLUMN std_word.name IS '단어명(한글). 전체에서 유일. 예) 고객';
COMMENT ON COLUMN std_word.abbreviation IS '영문약어. 대문자로 저장하며 전체에서 유일. 용어 영문약어를 만들 때 _로 잇는다. 예) CUST';
COMMENT ON COLUMN std_word.english_name IS '영문명(전체 표기). 유일하지 않다. 예) Customer';
COMMENT ON COLUMN std_word.description IS '설명 (선택)';
COMMENT ON COLUMN std_word.classifier IS '분류어 여부 Y/N. 분류어(번호, 명, 일자 등)만 도메인에 연결된다';
COMMENT ON COLUMN std_word.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN std_word.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_domain
COMMENT ON TABLE std_domain IS '표준 도메인. 분류어 단어별 데이터 타입·길이 규칙 (예: 번호V20 = 분류어 번호, VARCHAR(20))';
COMMENT ON COLUMN std_domain.id IS '도메인 ID (자동 증가 기본키)';
COMMENT ON COLUMN std_domain.name IS '도메인명. 전체에서 유일. 예) 번호V20';
COMMENT ON COLUMN std_domain.classifier_word_id IS '분류어 단어 ID (std_word.id). 분류어(classifier=Y) 단어만 가능';
COMMENT ON COLUMN std_domain.data_type IS '데이터 타입: CHAR, VARCHAR, DECIMAL, INTEGER, BIGINT, DATE, DATETIME, TEXT';
COMMENT ON COLUMN std_domain.data_length IS '길이. 길이를 쓰지 않는 타입이면 NULL';
COMMENT ON COLUMN std_domain.data_scale IS '소수점 자리수. DECIMAL만 값을 가지며 나머지 타입은 NULL';
COMMENT ON COLUMN std_domain.description IS '설명 (선택)';
COMMENT ON COLUMN std_domain.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN std_domain.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_term
COMMENT ON TABLE std_term IS '표준 용어. 단어를 순서대로 조합하고 도메인을 하나 연결한다 (예: 고객+번호 → 고객번호 / CUST_NO)';
COMMENT ON COLUMN std_term.id IS '용어 ID (자동 증가 기본키)';
COMMENT ON COLUMN std_term.name IS '용어명. 구성 단어명을 이어 붙여 만든다. 전체에서 유일. 예) 고객번호';
COMMENT ON COLUMN std_term.abbreviation IS '영문약어(물리명). 구성 단어 약어를 _로 이어 만든다. 전체에서 유일, 최대 64자. 예) CUST_NO';
COMMENT ON COLUMN std_term.domain_id IS '도메인 ID (std_domain.id). 컬럼 비표준 검출의 타입·길이 기준';
COMMENT ON COLUMN std_term.description IS '설명 (선택)';
COMMENT ON COLUMN std_term.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN std_term.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_term_word
COMMENT ON TABLE std_term_word IS '용어 구성 단어 (용어-단어 조인 테이블). 한 행이 용어의 seq번째 단어다';
COMMENT ON COLUMN std_term_word.term_id IS '용어 ID (std_term.id)';
COMMENT ON COLUMN std_term_word.seq IS '조합 순서 (1부터). 같은 단어가 여러 번 들어갈 수 있어 (term_id, seq)가 기본키다';
COMMENT ON COLUMN std_term_word.word_id IS '단어 ID (std_word.id)';

-- meta_table
COMMENT ON TABLE meta_table IS '테이블 정의. 실제 운영 테이블을 등록해 컬럼을 표준 용어에 매핑한다';
COMMENT ON COLUMN meta_table.id IS '테이블 정의 ID (자동 증가 기본키)';
COMMENT ON COLUMN meta_table.physical_name IS '테이블 물리명. 대문자로 저장하며 전체에서 유일, 64자 이하. 예) TB_CUSTOMER';
COMMENT ON COLUMN meta_table.logical_name IS '테이블 논리명(한글명). 예) 고객';
COMMENT ON COLUMN meta_table.description IS '설명 (선택)';
COMMENT ON COLUMN meta_table.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN meta_table.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- meta_column
COMMENT ON TABLE meta_column IS '컬럼 정의. 비표준 여부는 저장하지 않고 조회할 때 현재 용어·도메인 기준으로 계산한다';
COMMENT ON COLUMN meta_column.id IS '컬럼 정의 ID (자동 증가 기본키)';
COMMENT ON COLUMN meta_column.table_id IS '소속 테이블 ID (meta_table.id)';
COMMENT ON COLUMN meta_column.physical_name IS '컬럼 물리명. 대문자로 저장하며 테이블 안에서 유일. 예) CUST_NO';
COMMENT ON COLUMN meta_column.logical_name IS '컬럼 논리명(한글명). 예) 고객번호';
COMMENT ON COLUMN meta_column.term_id IS '매핑된 표준 용어 ID (std_term.id). NULL이면 용어 미매핑(NO_TERM)으로 검출된다';
COMMENT ON COLUMN meta_column.data_type IS '데이터 타입 (도메인과 같은 타입 이름)';
COMMENT ON COLUMN meta_column.data_length IS '길이. 길이를 쓰지 않는 타입이면 NULL';
COMMENT ON COLUMN meta_column.data_scale IS '소수점 자리수. DECIMAL만 값을 가진다';
COMMENT ON COLUMN meta_column.nullable_yn IS 'NULL 허용 여부 Y/N';
COMMENT ON COLUMN meta_column.primary_key_yn IS '기본키 여부 Y/N. 기본키 컬럼은 NULL을 허용할 수 없다';
COMMENT ON COLUMN meta_column.seq IS '테이블 안의 컬럼 순서 (1 이상, 같으면 ID 순)';
COMMENT ON COLUMN meta_column.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN meta_column.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';
COMMENT ON COLUMN meta_column.code_group_id IS '매핑된 공통 코드 그룹 ID (com_code_group.id). 선택, 비표준 검출과 무관';

-- app_user
COMMENT ON TABLE app_user IS '사용자 계정. 공개 가입 없이 관리자가 만들고, 삭제하지 않고 비활성으로 둔다';
COMMENT ON COLUMN app_user.id IS '사용자 ID (자동 증가 기본키)';
COMMENT ON COLUMN app_user.username IS '로그인 아이디. 소문자로 저장하며 전체에서 유일, 바꿀 수 없다. 예) hong.gildong';
COMMENT ON COLUMN app_user.password IS '비밀번호 해시 (BCrypt, {bcrypt} 접두어). 평문은 저장하지 않는다';
COMMENT ON COLUMN app_user.name IS '표시 이름. 예) 홍길동';
COMMENT ON COLUMN app_user.enabled IS '활성 여부 Y/N. N이면 로그인할 수 없다';
COMMENT ON COLUMN app_user.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN app_user.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- std_request
COMMENT ON TABLE std_request IS '표준 데이터 변경 신청. 단어·도메인·용어의 등록·수정·삭제 신청을 검토자가 승인하면 반영된다';
COMMENT ON COLUMN std_request.id IS '신청 ID (자동 증가 기본키)';
COMMENT ON COLUMN std_request.target_type IS '대상 종류: WORD, DOMAIN, TERM';
COMMENT ON COLUMN std_request.request_type IS '신청 종류: CREATE, UPDATE, DELETE';
COMMENT ON COLUMN std_request.target_id IS '대상 ID. 수정·삭제는 필수, 등록은 승인 후 새 대상 ID가 들어간다 (대상마다 테이블이 달라 FK 없음)';
COMMENT ON COLUMN std_request.target_updated_at IS '신청 시점 대상의 수정 일시. 승인 시 현재 값과 비교해 변경 충돌을 막는다';
COMMENT ON COLUMN std_request.payload IS '신청 내용 JSON (대상별 요청 본문). 삭제 신청은 NULL';
COMMENT ON COLUMN std_request.reason IS '신청 사유 (선택)';
COMMENT ON COLUMN std_request.status IS '상태: REQUESTED(대기), APPROVED(승인), REJECTED(반려), CANCELED(취소)';
COMMENT ON COLUMN std_request.requester_id IS '신청자 ID (app_user.id)';
COMMENT ON COLUMN std_request.reviewer_id IS '검토자 ID (app_user.id). 대기·취소 상태면 NULL';
COMMENT ON COLUMN std_request.review_comment IS '검토 의견. 반려 시 필수';
COMMENT ON COLUMN std_request.reviewed_at IS '처리 일시 (승인·반려·취소한 시각). 대기 상태면 NULL';
COMMENT ON COLUMN std_request.created_at IS '신청 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN std_request.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- change_history
COMMENT ON TABLE change_history IS '변경 이력. 표준 데이터·테이블/컬럼 정의·공통 코드가 바뀔 때마다 변경 전후 스냅샷을 남긴다';
COMMENT ON COLUMN change_history.id IS '이력 ID (자동 증가 기본키). 같은 시각의 변경도 이 값으로 순서를 구분한다';
COMMENT ON COLUMN change_history.target_type IS '대상 종류: WORD, DOMAIN, TERM, TABLE, COLUMN, CODE_GROUP, CODE_VALUE';
COMMENT ON COLUMN change_history.target_id IS '대상 ID (대상 테이블의 id)';
COMMENT ON COLUMN change_history.change_type IS '변경 종류: CREATE, UPDATE, DELETE';
COMMENT ON COLUMN change_history.before_data IS '변경 전 스냅샷 JSON. 등록이면 NULL';
COMMENT ON COLUMN change_history.after_data IS '변경 후 스냅샷 JSON. 삭제면 NULL';
COMMENT ON COLUMN change_history.changed_by IS '변경자 ID (app_user.id). 신청 승인 반영이면 승인자, 시스템 작업이면 NULL';
COMMENT ON COLUMN change_history.request_id IS '반영한 신청 ID (std_request.id). 직접 변경이면 NULL';
COMMENT ON COLUMN change_history.changed_at IS '변경 일시';

-- com_code_group
COMMENT ON TABLE com_code_group IS '공통 코드 그룹. 코드값 여러 개를 묶는 코드ID (예: ORDER_STAT 주문상태)';
COMMENT ON COLUMN com_code_group.id IS '코드 그룹 ID (자동 증가 기본키). 컬럼 매핑은 이 값으로 연결한다';
COMMENT ON COLUMN com_code_group.code_id IS '코드ID. 대문자로 저장하며 전체에서 유일. 예) ORDER_STAT';
COMMENT ON COLUMN com_code_group.code_name IS '코드명. 예) 주문상태';
COMMENT ON COLUMN com_code_group.description IS '설명 (선택)';
COMMENT ON COLUMN com_code_group.used_yn IS '사용 여부 Y/N. 미사용 그룹은 컬럼에 새로 매핑할 수 없다';
COMMENT ON COLUMN com_code_group.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN com_code_group.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- com_code_value
COMMENT ON TABLE com_code_value IS '공통 코드값. 코드 그룹에 속한 개별 값 (예: 01 접수)';
COMMENT ON COLUMN com_code_value.id IS '코드값 ID (자동 증가 기본키)';
COMMENT ON COLUMN com_code_value.group_id IS '소속 코드 그룹 ID (com_code_group.id)';
COMMENT ON COLUMN com_code_value.code_value IS '코드값. 대문자로 저장하며 그룹 안에서 유일. 예) 01, Y, ACTIVE';
COMMENT ON COLUMN com_code_value.value_name IS '코드값명. 예) 접수';
COMMENT ON COLUMN com_code_value.description IS '설명 (선택)';
COMMENT ON COLUMN com_code_value.sort_order IS '그룹 안의 정렬 순서 (1 이상, 같으면 ID 순)';
COMMENT ON COLUMN com_code_value.used_yn IS '사용 여부 Y/N';
COMMENT ON COLUMN com_code_value.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN com_code_value.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- app_menu
COMMENT ON TABLE app_menu IS '화면 메뉴. 상위(TOP)-하위(LEFT) 2단이며, 하위 메뉴의 경로가 권한의 이름이기도 하다';
COMMENT ON COLUMN app_menu.id IS '메뉴 ID (자동 증가 기본키)';
COMMENT ON COLUMN app_menu.parent_id IS '상위 메뉴 ID (app_menu.id). NULL이면 상위(TOP) 메뉴';
COMMENT ON COLUMN app_menu.name IS '메뉴명. 예) 표준 단어';
COMMENT ON COLUMN app_menu.path IS '화면 경로. 하위 메뉴만 가지며 전체에서 유일. 예) /words';
COMMENT ON COLUMN app_menu.icon IS '아이콘 이름 (프론트엔드가 아이콘으로 바꾼다). 예) book';
COMMENT ON COLUMN app_menu.sort_order IS '같은 단계 안의 정렬 순서 (같으면 ID 순)';
COMMENT ON COLUMN app_menu.used_yn IS '사용 여부 Y/N. 미사용 메뉴는 화면에 나오지 않는다';
COMMENT ON COLUMN app_menu.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN app_menu.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- app_user_menu
COMMENT ON TABLE app_user_menu IS '사용자별 메뉴 지정. 행이 있는 사용자는 지정된 메뉴만 보고, 없으면 역할 기본 규칙대로 본다';
COMMENT ON COLUMN app_user_menu.id IS '메뉴 지정 ID (자동 증가 기본키)';
COMMENT ON COLUMN app_user_menu.user_id IS '사용자 ID (app_user.id)';
COMMENT ON COLUMN app_user_menu.menu_id IS '메뉴 ID (app_menu.id). 하위(LEFT) 메뉴를 지정한다';
COMMENT ON COLUMN app_user_menu.created_at IS '지정 일시 (등록 시 자동 기록). 수정하지 않고 통째로 교체한다';

-- app_role
COMMENT ON TABLE app_role IS '역할. 역할별 메뉴 권한을 묶고 사용자에게 여러 개 부여한다 (권한은 합집합)';
COMMENT ON COLUMN app_role.id IS '역할 ID (자동 증가 기본키)';
COMMENT ON COLUMN app_role.code IS '역할 코드. 대문자로 저장하며 전체에서 유일. JWT roles 클레임에 들어간다. 예) ADMIN';
COMMENT ON COLUMN app_role.name IS '역할명. 예) 관리자';
COMMENT ON COLUMN app_role.description IS '설명 (선택)';
COMMENT ON COLUMN app_role.all_permissions_yn IS '전권 여부 Y/N. Y면 메뉴 권한과 무관하게 모든 기능을 쓸 수 있다';
COMMENT ON COLUMN app_role.used_yn IS '사용 여부 Y/N. 미사용 역할의 권한은 무시된다';
COMMENT ON COLUMN app_role.built_in_yn IS '기본 제공 역할 여부 Y/N. Y면 삭제·코드 변경·전권 변경이 막힌다';
COMMENT ON COLUMN app_role.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN app_role.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- app_role_menu
COMMENT ON TABLE app_role_menu IS '역할별 메뉴 권한. 역할마다 메뉴의 조회·등록·수정·삭제 권한을 둔다';
COMMENT ON COLUMN app_role_menu.id IS '역할 메뉴 권한 ID (자동 증가 기본키)';
COMMENT ON COLUMN app_role_menu.role_id IS '역할 ID (app_role.id)';
COMMENT ON COLUMN app_role_menu.menu_id IS '메뉴 ID (app_menu.id). 하위(LEFT) 메뉴에 권한을 준다';
COMMENT ON COLUMN app_role_menu.read_yn IS '조회 권한 Y/N. N이면 메뉴가 보이지 않는다';
COMMENT ON COLUMN app_role_menu.create_yn IS '등록 권한 Y/N';
COMMENT ON COLUMN app_role_menu.update_yn IS '수정 권한 Y/N';
COMMENT ON COLUMN app_role_menu.delete_yn IS '삭제 권한 Y/N';
COMMENT ON COLUMN app_role_menu.created_at IS '생성 일시 (등록 시 자동 기록). 수정하지 않고 통째로 교체한다';

-- app_user_role
COMMENT ON TABLE app_user_role IS '사용자 역할 (사용자-역할 조인 테이블)';
COMMENT ON COLUMN app_user_role.user_id IS '사용자 ID (app_user.id)';
COMMENT ON COLUMN app_user_role.role_id IS '역할 ID (app_role.id)';

-- phm_prescription
COMMENT ON TABLE phm_prescription IS '처방 마스터. 처방 → 조제 → 검수 → 불출 → 반납으로 이어지는 약제 업무의 시작';
COMMENT ON COLUMN phm_prescription.id IS '처방 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_prescription.prescription_no IS '처방전번호. OCS가 발급한 번호로 대문자로 저장하며 전체에서 유일';
COMMENT ON COLUMN phm_prescription.patient_no IS '환자번호(등록번호)';
COMMENT ON COLUMN phm_prescription.patient_name IS '환자명';
COMMENT ON COLUMN phm_prescription.department_code IS '처방 진료과 코드 (대문자). 예) IM(내과)';
COMMENT ON COLUMN phm_prescription.prescriber_name IS '처방의 이름';
COMMENT ON COLUMN phm_prescription.prescription_type IS '처방 구분: OUTPATIENT(외래), INPATIENT(입원), DISCHARGE(퇴원), EMERGENCY(응급)';
COMMENT ON COLUMN phm_prescription.status IS '처방 상태: ORDERED(처방), DISPENSED(조제됨), CANCELLED(취소). 취소는 조제 전에만 가능';
COMMENT ON COLUMN phm_prescription.prescribed_at IS '처방 일시';
COMMENT ON COLUMN phm_prescription.remark IS '비고 (선택)';
COMMENT ON COLUMN phm_prescription.cancelled_at IS '취소 일시 (취소한 처방만)';
COMMENT ON COLUMN phm_prescription.cancel_reason IS '취소 사유 (취소한 처방만)';
COMMENT ON COLUMN phm_prescription.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN phm_prescription.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_prescription_item
COMMENT ON TABLE phm_prescription_item IS '처방 품목 (처방의 하위). 한 행이 처방 약품 하나다';
COMMENT ON COLUMN phm_prescription_item.id IS '처방 품목 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_prescription_item.prescription_id IS '처방 ID (phm_prescription.id)';
COMMENT ON COLUMN phm_prescription_item.line_no IS '처방 안의 순번 (1부터, 요청 순서대로)';
COMMENT ON COLUMN phm_prescription_item.drug_code IS '약품코드 (대문자). 예) AMXCLN500';
COMMENT ON COLUMN phm_prescription_item.drug_name IS '약품명. 예) 아목시실린캡슐 500mg';
COMMENT ON COLUMN phm_prescription_item.dose IS '1회 투여량. 예) 500';
COMMENT ON COLUMN phm_prescription_item.dose_unit IS '투여 단위 (대문자). 예) MG, ML, TAB';
COMMENT ON COLUMN phm_prescription_item.route IS '투여 경로 (대문자). 예) PO(경구), IV(정맥)';
COMMENT ON COLUMN phm_prescription_item.frequency IS '투여 빈도 (대문자). 예) TID(1일 3회)';
COMMENT ON COLUMN phm_prescription_item.days IS '투여 일수 (1~365)';
COMMENT ON COLUMN phm_prescription_item.total_quantity IS '총 처방량 (조제 단위 기준). 예) 21';
COMMENT ON COLUMN phm_prescription_item.usage_note IS '용법 설명 (선택). 예) 식후 30분';

-- phm_dispensing
COMMENT ON TABLE phm_dispensing IS '조제 마스터. 처방 하나에 검수 불합격(REJECTED)이 아닌 조제는 하나뿐이다';
COMMENT ON COLUMN phm_dispensing.id IS '조제 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_dispensing.prescription_id IS '처방 ID (phm_prescription.id)';
COMMENT ON COLUMN phm_dispensing.status IS '조제 상태: DISPENSED(조제), INSPECTED(검수 합격), REJECTED(검수 불합격), ISSUED(불출)';
COMMENT ON COLUMN phm_dispensing.dispensed_by IS '조제자 로그인 아이디';
COMMENT ON COLUMN phm_dispensing.dispensed_at IS '조제 일시';
COMMENT ON COLUMN phm_dispensing.remark IS '비고 (선택)';
COMMENT ON COLUMN phm_dispensing.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN phm_dispensing.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_dispensing_item
COMMENT ON TABLE phm_dispensing_item IS '조제 품목 (조제의 하위). 처방 품목마다 정확히 하나씩 있다';
COMMENT ON COLUMN phm_dispensing_item.id IS '조제 품목 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_dispensing_item.dispensing_id IS '조제 ID (phm_dispensing.id)';
COMMENT ON COLUMN phm_dispensing_item.prescription_item_id IS '처방 품목 ID (phm_prescription_item.id)';
COMMENT ON COLUMN phm_dispensing_item.quantity IS '조제 수량 (0보다 크다)';
COMMENT ON COLUMN phm_dispensing_item.lot_no IS '제조번호(로트) (선택)';
COMMENT ON COLUMN phm_dispensing_item.expiry_date IS '유효기간 만료일 (선택)';

-- phm_inspection
COMMENT ON TABLE phm_inspection IS '검수 마스터. 조제 하나에 검수 하나다';
COMMENT ON COLUMN phm_inspection.id IS '검수 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_inspection.dispensing_id IS '조제 ID (phm_dispensing.id). 전체에서 유일';
COMMENT ON COLUMN phm_inspection.result IS '검수 결과: PASS(전 품목 합격), FAIL(불합격 품목 있음). 품목 결과로 계산한다';
COMMENT ON COLUMN phm_inspection.inspected_by IS '검수자 로그인 아이디';
COMMENT ON COLUMN phm_inspection.inspected_at IS '검수 일시';
COMMENT ON COLUMN phm_inspection.remark IS '비고 (선택)';
COMMENT ON COLUMN phm_inspection.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN phm_inspection.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_inspection_item
COMMENT ON TABLE phm_inspection_item IS '검수 품목 (검수의 하위). 조제 품목마다 정확히 하나씩 있다';
COMMENT ON COLUMN phm_inspection_item.id IS '검수 품목 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_inspection_item.inspection_id IS '검수 ID (phm_inspection.id)';
COMMENT ON COLUMN phm_inspection_item.dispensing_item_id IS '조제 품목 ID (phm_dispensing_item.id)';
COMMENT ON COLUMN phm_inspection_item.result IS '품목 검수 결과: PASS, FAIL';
COMMENT ON COLUMN phm_inspection_item.defect_reason IS '불량 사유. 불합격(FAIL)이면 필수';

-- phm_issue
COMMENT ON TABLE phm_issue IS '불출 마스터. 검수 합격한 조제 하나에 불출 하나다';
COMMENT ON COLUMN phm_issue.id IS '불출 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_issue.dispensing_id IS '조제 ID (phm_dispensing.id). 전체에서 유일';
COMMENT ON COLUMN phm_issue.destination_code IS '불출처 코드 (대문자, 병동·부서·외래 창구). 예) W71';
COMMENT ON COLUMN phm_issue.destination_name IS '불출처 이름. 예) 71병동';
COMMENT ON COLUMN phm_issue.receiver_name IS '수령자 이름';
COMMENT ON COLUMN phm_issue.issued_by IS '불출자 로그인 아이디';
COMMENT ON COLUMN phm_issue.issued_at IS '불출 일시';
COMMENT ON COLUMN phm_issue.remark IS '비고 (선택)';
COMMENT ON COLUMN phm_issue.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN phm_issue.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_issue_item
COMMENT ON TABLE phm_issue_item IS '불출 품목 (불출의 하위). 조제 품목을 수량 그대로 옮긴다';
COMMENT ON COLUMN phm_issue_item.id IS '불출 품목 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_issue_item.issue_id IS '불출 ID (phm_issue.id)';
COMMENT ON COLUMN phm_issue_item.dispensing_item_id IS '조제 품목 ID (phm_dispensing_item.id)';
COMMENT ON COLUMN phm_issue_item.quantity IS '불출 수량 (= 조제 수량)';

-- phm_return
COMMENT ON TABLE phm_return IS '반납 마스터. 불출 하나에 여러 번, 일부 품목만 반납할 수 있다';
COMMENT ON COLUMN phm_return.id IS '반납 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_return.issue_id IS '불출 ID (phm_issue.id)';
COMMENT ON COLUMN phm_return.reason IS '반납 사유. 예) 퇴원, 처방 중단';
COMMENT ON COLUMN phm_return.returned_by IS '반납자 로그인 아이디';
COMMENT ON COLUMN phm_return.returned_at IS '반납 일시';
COMMENT ON COLUMN phm_return.remark IS '비고 (선택)';
COMMENT ON COLUMN phm_return.created_at IS '생성 일시 (등록 시 자동 기록)';
COMMENT ON COLUMN phm_return.updated_at IS '최종 수정 일시 (등록·수정 시 자동 기록)';

-- phm_return_item
COMMENT ON TABLE phm_return_item IS '반납 품목 (반납의 하위)';
COMMENT ON COLUMN phm_return_item.id IS '반납 품목 ID (자동 증가 기본키)';
COMMENT ON COLUMN phm_return_item.return_id IS '반납 ID (phm_return.id)';
COMMENT ON COLUMN phm_return_item.issue_item_id IS '불출 품목 ID (phm_issue_item.id)';
COMMENT ON COLUMN phm_return_item.quantity IS '반납 수량 (0보다 크다). 불출 품목별 반납 합계는 불출 수량 이하';
COMMENT ON COLUMN phm_return_item.disposition IS '반납 후 처리: RESTOCK(재입고), DISPOSE(폐기)';
