package com.step.dictionary.global.error;

import org.springframework.http.HttpStatus;

/**
 * 오류 코드 목록. 응답 본문의 {@code code}에는 enum 이름이, {@code message}에는 기본 메시지가 들어간다.
 *
 * <p>HTTP 상태 사용 기준:
 * <ul>
 *   <li>400 BAD_REQUEST: 입력값 자체가 규칙에 맞지 않음 (형식, 타입/길이 조합, 조합 규칙)</li>
 *   <li>404 NOT_FOUND: 경로나 본문으로 지정한 대상이 없음</li>
 *   <li>409 CONFLICT: 입력은 올바르지만 현재 데이터 상태와 충돌함 (중복, 참조 중)</li>
 * </ul>
 * 새 오류가 필요하면 여기에 HTTP 상태와 함께 추가한다.
 */
public enum ErrorCode {

    // ---- 공통 ----
    /** Bean Validation 실패, JSON 파싱 실패, 경로/쿼리 파라미터 타입 불일치. */
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),

    // ---- 표준 단어 (1단계) ----
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "단어를 찾을 수 없습니다."),
    DUPLICATE_WORD_NAME(HttpStatus.CONFLICT, "이미 등록된 단어명입니다."),
    /** 대문자로 정규화한 뒤 비교하므로 cust와 CUST는 같은 약어다. */
    DUPLICATE_WORD_ABBREVIATION(HttpStatus.CONFLICT, "이미 등록된 영문약어입니다."),
    /** 도메인의 분류어로 쓰이거나 용어의 구성 단어로 쓰이는 단어를 삭제하려 할 때. */
    WORD_IN_USE(HttpStatus.CONFLICT, "다른 곳에서 참조 중인 단어는 삭제할 수 없습니다."),
    /** 도메인이 분류어로 참조하는 단어의 분류어 여부를 N으로 바꾸려 할 때. */
    CLASSIFIER_WORD_IN_USE(HttpStatus.CONFLICT, "도메인에서 사용 중인 단어는 분류어를 해제할 수 없습니다."),
    /** 용어명/영문약어가 단어에서 생성되므로, 용어에서 쓰는 단어의 이름이나 약어를 바꾸면 용어와 어긋난다. */
    WORD_USED_BY_TERM(HttpStatus.CONFLICT, "용어에서 사용 중인 단어는 단어명과 영문약어를 바꿀 수 없습니다."),
    /** 도메인의 분류어로 분류어가 아닌 단어를 지정했을 때. */
    NOT_CLASSIFIER_WORD(HttpStatus.BAD_REQUEST, "분류어가 아닌 단어입니다."),

    // ---- 표준 도메인 (2단계) ----
    DOMAIN_NOT_FOUND(HttpStatus.NOT_FOUND, "도메인을 찾을 수 없습니다."),
    DUPLICATE_DOMAIN_NAME(HttpStatus.CONFLICT, "이미 등록된 도메인명입니다."),
    /** DataType 규칙에 맞지 않는 길이/소수점 조합 (예: DATE에 길이 지정, VARCHAR에 소수점 지정). */
    INVALID_DOMAIN_SPEC(HttpStatus.BAD_REQUEST, "데이터 타입에 맞지 않는 길이 또는 소수점 자리수입니다."),
    /** 용어가 참조하는 도메인을 삭제하려 할 때. */
    DOMAIN_IN_USE(HttpStatus.CONFLICT, "용어에서 사용 중인 도메인은 삭제할 수 없습니다."),
    /** 용어의 마지막 단어와 도메인 분류어가 같아야 하므로, 용어가 참조하는 도메인은 분류어를 바꿀 수 없다. */
    DOMAIN_CLASSIFIER_IN_USE(HttpStatus.CONFLICT, "용어에서 사용 중인 도메인은 분류어를 바꿀 수 없습니다."),

    // ---- 표준 용어 (3단계) ----
    TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "용어를 찾을 수 없습니다."),
    /** 생성된 용어명이 기존 용어와 같을 때 (예: 고객+번호와 고객번+호처럼 다른 조합이 같은 이름을 만드는 경우 포함). */
    DUPLICATE_TERM_NAME(HttpStatus.CONFLICT, "이미 등록된 용어명입니다."),
    DUPLICATE_TERM_ABBREVIATION(HttpStatus.CONFLICT, "이미 등록된 용어 영문약어입니다."),
    TERM_LAST_WORD_NOT_CLASSIFIER(HttpStatus.BAD_REQUEST, "용어의 마지막 단어는 분류어여야 합니다."),
    TERM_DOMAIN_MISMATCH(HttpStatus.BAD_REQUEST, "용어의 마지막 단어와 도메인의 분류어가 다릅니다."),
    /** 영문약어는 컬럼명 기준이라 MySQL 식별자 한도(64자)를 넘으면 안 된다. */
    TERM_TOO_LONG(HttpStatus.BAD_REQUEST, "용어명은 200자, 영문약어는 64자를 넘을 수 없습니다."),
    /** 컬럼 정의에 매핑된 용어를 삭제하려 할 때. */
    TERM_IN_USE(HttpStatus.CONFLICT, "컬럼에 매핑된 용어는 삭제할 수 없습니다."),

    // ---- 테이블 정의 (4단계) ----
    TABLE_NOT_FOUND(HttpStatus.NOT_FOUND, "테이블을 찾을 수 없습니다."),
    DUPLICATE_TABLE_NAME(HttpStatus.CONFLICT, "이미 등록된 테이블 물리명입니다."),

    // ---- 컬럼 정의 (4단계) ----
    /** 컬럼이 없거나, 경로의 테이블에 속하지 않은 컬럼일 때. */
    COLUMN_NOT_FOUND(HttpStatus.NOT_FOUND, "컬럼을 찾을 수 없습니다."),
    DUPLICATE_COLUMN_NAME(HttpStatus.CONFLICT, "테이블에 이미 등록된 컬럼 물리명입니다."),
    /** 도메인과 같은 DataType 규칙으로 검증한다. */
    INVALID_COLUMN_SPEC(HttpStatus.BAD_REQUEST, "데이터 타입에 맞지 않는 길이 또는 소수점 자리수입니다."),
    PRIMARY_KEY_NULLABLE(HttpStatus.BAD_REQUEST, "기본키 컬럼은 NULL을 허용할 수 없습니다."),

    // ---- 인증/권한 (5단계) ----
    /** 보호된 API를 토큰 없이 호출했을 때. {@code SecurityErrorHandler}가 응답한다. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    /** 토큰의 서명이 틀리거나, 만료됐거나, 형식이 잘못됐을 때. 클라이언트는 다시 로그인해야 한다. */
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "인증 토큰이 유효하지 않거나 만료되었습니다."),
    /** 로그인은 했지만 역할이 부족할 때 (예: USER가 단어 등록). {@code SecurityErrorHandler}가 응답한다. */
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    /** 아이디가 없거나 비밀번호가 틀렸을 때. 어느 쪽인지 구분하지 않는다. */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    /** 비밀번호는 맞지만 비활성화된 계정일 때. */
    USER_DISABLED(HttpStatus.FORBIDDEN, "비활성화된 계정입니다."),
    /** 내 비밀번호 변경에서 현재 비밀번호가 틀렸을 때. */
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."),

    // ---- 사용자 관리 (5단계) ----
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    /** 소문자로 정규화한 뒤 비교하므로 Hong과 hong은 같은 아이디다. */
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    /** 마지막 활성 관리자를 강등하거나 비활성화하려 할 때. 관리자가 없으면 아무도 계정을 관리할 수 없다. */
    LAST_ADMIN_REQUIRED(HttpStatus.CONFLICT, "활성 관리자가 최소 한 명은 있어야 합니다."),

    // ---- 신청/승인 (6단계) ----
    /** 신청이 없거나, USER가 남의 신청을 조회·취소하려 할 때. 남의 신청 존재 여부를 드러내지 않는다. */
    REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "신청을 찾을 수 없습니다."),
    /** 신청 종류와 대상 ID/신청 내용의 조합이 맞지 않을 때 (예: 등록 신청에 대상 ID, 삭제 신청에 신청 내용). */
    INVALID_CHANGE_REQUEST(HttpStatus.BAD_REQUEST, "신청 종류에 맞지 않는 대상 ID 또는 신청 내용입니다."),
    /** 같은 대상에 처리 대기 중인 수정/삭제 신청이 이미 있을 때. */
    REQUEST_ALREADY_PENDING(HttpStatus.CONFLICT, "처리 대기 중인 신청이 이미 있는 대상입니다."),
    /** 이미 승인·반려·취소된 신청을 다시 처리하려 할 때. */
    REQUEST_NOT_PENDING(HttpStatus.CONFLICT, "처리 대기 중인 신청이 아닙니다."),
    /** 수정/삭제 신청 이후 대상이 바뀌거나 삭제되어 신청 내용이 최신 상태를 기준으로 하지 않을 때. */
    REQUEST_TARGET_CHANGED(HttpStatus.CONFLICT, "신청 이후 대상이 변경되어 승인할 수 없습니다. 반려 후 다시 신청해야 합니다."),
    /** 신청자가 아닌 사용자(관리자 포함)가 취소하려 할 때. 관리자는 반려를 쓴다. */
    REQUEST_NOT_OWNED(HttpStatus.FORBIDDEN, "본인의 신청만 취소할 수 있습니다."),

    // ---- 변경 이력 (7단계) ----
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "변경 이력을 찾을 수 없습니다."),

    // ---- 엑셀 업로드/다운로드 (8단계) ----
    /** .xlsx가 아니거나 손상된 파일. 구형 .xls도 여기에 해당한다. */
    EXCEL_INVALID_FILE(HttpStatus.BAD_REQUEST, "엑셀(.xlsx) 파일을 읽을 수 없습니다."),
    /** 첫 행의 헤더가 양식과 다를 때. fieldErrors에 필요한 헤더 목록이 들어간다. */
    EXCEL_INVALID_HEADER(HttpStatus.BAD_REQUEST, "엑셀 양식의 헤더가 다릅니다. 양식 파일을 내려받아 사용해 주세요."),
    /** 헤더 아래에 값이 있는 행이 하나도 없을 때. */
    EXCEL_EMPTY(HttpStatus.BAD_REQUEST, "엑셀 파일에 데이터 행이 없습니다."),
    /** 업로드 1,000행 또는 다운로드 10,000건을 넘을 때. */
    EXCEL_TOO_MANY_ROWS(HttpStatus.BAD_REQUEST, "행이 너무 많습니다. 업로드는 1,000행, 다운로드는 10,000건까지 가능합니다."),
    /** 오류 행이 하나라도 있을 때. 전체를 반영하지 않으며 fieldErrors에 행 번호별 사유가 들어간다. */
    EXCEL_INVALID_ROWS(HttpStatus.BAD_REQUEST, "오류가 있는 행이 있어 아무것도 반영하지 않았습니다."),
    /** spring.servlet.multipart.max-file-size(10MB)를 넘는 파일. */
    EXCEL_FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "업로드 파일이 너무 큽니다 (최대 10MB)."),

    // ---- 공통 코드 (9단계) ----
    CODE_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "코드 그룹을 찾을 수 없습니다."),
    /** 대문자로 정규화한 뒤 비교하므로 order_stat과 ORDER_STAT은 같은 코드ID다. */
    DUPLICATE_CODE_ID(HttpStatus.CONFLICT, "이미 등록된 코드ID입니다."),
    /** 컬럼 정의에 매핑된 코드 그룹을 삭제하려 할 때. 컬럼의 매핑을 먼저 해제해야 한다. */
    CODE_GROUP_IN_USE(HttpStatus.CONFLICT, "컬럼에 매핑된 코드 그룹은 삭제할 수 없습니다."),
    /** 사용 여부가 N인 코드 그룹을 컬럼에 새로 매핑하려 할 때. 이미 매핑된 컬럼은 매핑을 유지한 채 수정할 수 있다. */
    CODE_GROUP_NOT_USED(HttpStatus.CONFLICT, "사용하지 않는 코드 그룹은 컬럼에 매핑할 수 없습니다."),
    /** 코드값이 없거나, 경로의 코드 그룹에 속하지 않은 코드값일 때. */
    CODE_VALUE_NOT_FOUND(HttpStatus.NOT_FOUND, "코드값을 찾을 수 없습니다."),
    /** 대문자로 정규화한 뒤 그룹 안에서 비교한다. 다른 그룹에는 같은 코드값이 있어도 된다. */
    DUPLICATE_CODE_VALUE(HttpStatus.CONFLICT, "코드 그룹에 이미 등록된 코드값입니다."),

    // ---- 메뉴 (10단계) ----
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    MENU_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "상위 메뉴를 찾을 수 없습니다."),
    /** 하위 메뉴를 상위로 지정했거나, 하위를 가진 메뉴를 다른 메뉴 밑으로 옮기려 할 때. 메뉴는 2단까지만 둔다. */
    INVALID_MENU_PARENT(HttpStatus.BAD_REQUEST, "메뉴는 상위-하위 2단까지만 만들 수 있습니다."),
    /** 상위 메뉴에 경로를 지정했거나, 하위 메뉴에 경로가 없을 때. */
    INVALID_MENU_PATH(HttpStatus.BAD_REQUEST, "상위 메뉴는 화면 경로를 둘 수 없고, 하위 메뉴는 화면 경로가 필요합니다."),
    DUPLICATE_MENU_PATH(HttpStatus.CONFLICT, "이미 등록된 메뉴 경로입니다."),
    /** 하위 메뉴가 있는 메뉴를 삭제하려 할 때. 하위를 먼저 옮기거나 지워야 한다. */
    MENU_HAS_CHILDREN(HttpStatus.CONFLICT, "하위 메뉴가 있는 메뉴는 삭제할 수 없습니다.");

    /** 응답 HTTP 상태. */
    private final HttpStatus status;
    /** 응답 본문의 message에 들어가는 기본 메시지. */
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
