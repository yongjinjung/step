package com.step.dictionary.role;

/**
 * 메뉴별 권한 동작.
 *
 * <p>요청이 어느 동작인지는 HTTP 메서드로 정한다 (GET=조회, POST=등록, PUT/PATCH=수정, DELETE=삭제).
 * 예외가 필요한 경로는 {@code ApiPermissionRules}에 따로 적는다 (예: 신청 승인은 수정, 엑셀 다운로드는 조회).
 */
public enum PermissionAction {

    /** 조회. 이 권한이 없으면 메뉴 자체가 보이지 않는다. */
    READ,
    /** 등록. */
    CREATE,
    /** 수정. */
    UPDATE,
    /** 삭제. */
    DELETE
}
