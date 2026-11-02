package com.step.dictionary.role;

/**
 * 권한 기준이 되는 메뉴 경로.
 *
 * <p>권한은 메뉴에 붙고, API 요청은 {@code ApiPermissionRules}가 이 경로들로 옮겨 검사한다.
 * 즉 메뉴의 화면 경로는 화면 주소이면서 권한의 이름이기도 하다.
 * <b>메뉴 관리 화면에서 이 경로를 바꾸면 그 메뉴에 걸린 권한이 API와 이어지지 않는다</b>
 * (전권 역할만 쓸 수 있게 된다). 화면을 새로 만들 때만 새 경로를 쓴다.
 */
public final class MenuPaths {

    public static final String WORDS = "/words";
    public static final String DOMAINS = "/domains";
    public static final String TERMS = "/terms";
    public static final String CODES = "/codes";
    public static final String TABLES = "/tables";
    public static final String NON_STANDARD_COLUMNS = "/non-standard-columns";
    public static final String REQUESTS = "/requests";
    public static final String HISTORIES = "/histories";
    public static final String USERS = "/users";
    public static final String MENUS = "/menus";
    public static final String MENU_PERMISSIONS = "/menu-permissions";
    public static final String ROLES = "/roles";

    /** 인스턴스 생성 방지. */
    private MenuPaths() {
    }
}
