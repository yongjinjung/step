package com.step.dictionary.global.security;

import com.step.dictionary.role.MenuPaths;
import com.step.dictionary.role.PermissionAction;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * API 요청을 "어느 메뉴의 어떤 권한이 필요한가"로 옮기는 규칙표.
 *
 * <p>권한은 메뉴에 붙어 있으므로(역할 → 메뉴별 조회/등록/수정/삭제) 요청 경로를 메뉴 경로로 바꿔야 검사할 수 있다.
 * 기본은 HTTP 메서드로 동작을 정하고(GET=조회, POST=등록, PUT/PATCH=수정, DELETE=삭제),
 * 예외가 필요한 경로만 위쪽에 따로 적는다.
 *
 * <p>규칙은 <b>위에서부터 먼저 일치하는 것</b>이 적용된다. 한 규칙이 여러 조건을 가지면 그중 하나만 만족해도 허용이다
 * (예: 표준 데이터 엑셀 업로드는 직접 등록 권한이나 변경 신청 권한 중 하나면 된다).
 *
 * <p><b>새 API를 추가하면 여기에 규칙을 더해야 한다.</b> 규칙이 없는 경로는 (전권 역할이 아니면) 모두 막힌다.
 * 권한 검사 자체는 {@link MenuAuthorizationManager}가 한다.
 */
public final class ApiPermissionRules {

    /** 요청 하나에 필요한 권한 (메뉴 + 동작). */
    public record Requirement(String menuPath, PermissionAction action) {
    }

    /**
     * 규칙 하나.
     *
     * @param method  HTTP 메서드. null이면 모든 메서드
     * @param pattern 경로 패턴 (Ant 스타일: {@code /api/words/**})
     * @param anyOf   필요한 권한. 하나라도 만족하면 허용한다
     */
    private record Rule(HttpMethod method, String pattern, List<Requirement> anyOf) {
    }

    private static final PathMatcher MATCHER = new AntPathMatcher();
    private static final List<Rule> RULES = rules();

    /** 인스턴스 생성 방지. */
    private ApiPermissionRules() {
    }

    /**
     * 요청에 필요한 권한을 찾는다.
     *
     * @return 필요한 권한 목록 (하나라도 만족하면 허용). 규칙이 없으면 빈 목록 = 허용하지 않음
     */
    public static List<Requirement> requirementsFor(String method, String path) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        return RULES.stream()
                .filter(rule -> (rule.method() == null || rule.method().equals(httpMethod))
                        && MATCHER.match(rule.pattern(), path))
                .findFirst()
                .map(Rule::anyOf)
                .orElseGet(List::of);
    }

    private static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();

        // ---- 예외 규칙 (메서드만으로 동작을 정할 수 없는 경로) ----
        // 신청 승인·반려는 신청 메뉴의 수정 권한으로 본다. 취소는 신청자 본인만 하므로(서비스에서 검사) 조회 권한이면 된다.
        rules.add(new Rule(HttpMethod.POST, "/api/requests/*/approve", req(MenuPaths.REQUESTS, PermissionAction.UPDATE)));
        rules.add(new Rule(HttpMethod.POST, "/api/requests/*/reject", req(MenuPaths.REQUESTS, PermissionAction.UPDATE)));
        rules.add(new Rule(HttpMethod.POST, "/api/requests/*/cancel", req(MenuPaths.REQUESTS, PermissionAction.READ)));

        // 엑셀은 대상 데이터의 메뉴 권한으로 본다. 표준 데이터 업로드는 등록 권한이 없으면 변경 신청으로 처리되므로
        // 신청 등록 권한만 있어도 허용한다 (StandardExcelService가 권한에 맞춰 반영/신청을 고른다).
        rules.add(new Rule(HttpMethod.GET, "/api/excel/words/**", req(MenuPaths.WORDS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/words", req(MenuPaths.WORDS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.POST, "/api/excel/words", anyOf(
                new Requirement(MenuPaths.WORDS, PermissionAction.CREATE),
                new Requirement(MenuPaths.REQUESTS, PermissionAction.CREATE))));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/domains/**", req(MenuPaths.DOMAINS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/domains", req(MenuPaths.DOMAINS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.POST, "/api/excel/domains", anyOf(
                new Requirement(MenuPaths.DOMAINS, PermissionAction.CREATE),
                new Requirement(MenuPaths.REQUESTS, PermissionAction.CREATE))));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/terms/**", req(MenuPaths.TERMS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/terms", req(MenuPaths.TERMS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.POST, "/api/excel/terms", anyOf(
                new Requirement(MenuPaths.TERMS, PermissionAction.CREATE),
                new Requirement(MenuPaths.REQUESTS, PermissionAction.CREATE))));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/tables/**", req(MenuPaths.TABLES, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.GET, "/api/excel/tables", req(MenuPaths.TABLES, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.POST, "/api/excel/tables", req(MenuPaths.TABLES, PermissionAction.CREATE)));

        // 비표준 컬럼 검색은 전용 메뉴 또는 테이블 정의 메뉴의 조회 권한이면 된다.
        rules.add(new Rule(HttpMethod.GET, "/api/columns/non-standard", anyOf(
                new Requirement(MenuPaths.NON_STANDARD_COLUMNS, PermissionAction.READ),
                new Requirement(MenuPaths.TABLES, PermissionAction.READ))));

        // 사용자별 메뉴 권한은 메뉴 권한 화면의 권한으로 본다 (사용자 관리와 나눠 맡길 수 있게).
        rules.add(new Rule(HttpMethod.GET, "/api/users/*/menus", req(MenuPaths.MENU_PERMISSIONS, PermissionAction.READ)));
        rules.add(new Rule(HttpMethod.PUT, "/api/users/*/menus",
                req(MenuPaths.MENU_PERMISSIONS, PermissionAction.UPDATE)));

        // 처방 취소는 등록된 처방의 상태를 바꾸므로 수정 권한으로 본다.
        rules.add(new Rule(HttpMethod.POST, "/api/prescriptions/*/cancel",
                req(MenuPaths.PRESCRIPTIONS, PermissionAction.UPDATE)));

        // ---- 기본 규칙: 메서드로 동작을 정한다 ----
        rules.addAll(crud("/api/words/**", MenuPaths.WORDS));
        rules.addAll(crud("/api/domains/**", MenuPaths.DOMAINS));
        rules.addAll(crud("/api/terms/**", MenuPaths.TERMS));
        rules.addAll(crud("/api/code-groups/**", MenuPaths.CODES));
        rules.addAll(crud("/api/tables/**", MenuPaths.TABLES));
        rules.addAll(crud("/api/columns/**", MenuPaths.TABLES));
        rules.addAll(crud("/api/requests/**", MenuPaths.REQUESTS));
        rules.addAll(crud("/api/histories/**", MenuPaths.HISTORIES));
        rules.addAll(crud("/api/users/**", MenuPaths.USERS));
        rules.addAll(crud("/api/menus/**", MenuPaths.MENUS));
        rules.addAll(crud("/api/roles/**", MenuPaths.ROLES));
        rules.addAll(crud("/api/prescriptions/**", MenuPaths.PRESCRIPTIONS));
        rules.addAll(crud("/api/dispensings/**", MenuPaths.DISPENSINGS));
        rules.addAll(crud("/api/inspections/**", MenuPaths.INSPECTIONS));
        rules.addAll(crud("/api/issues/**", MenuPaths.ISSUES));
        rules.addAll(crud("/api/returns/**", MenuPaths.RETURNS));
        return List.copyOf(rules);
    }

    /** 경로 하나에 대해 메서드별 조회/등록/수정/삭제 규칙을 만든다. */
    private static List<Rule> crud(String pattern, String menuPath) {
        return List.of(
                new Rule(HttpMethod.GET, pattern, req(menuPath, PermissionAction.READ)),
                new Rule(HttpMethod.POST, pattern, req(menuPath, PermissionAction.CREATE)),
                new Rule(HttpMethod.PUT, pattern, req(menuPath, PermissionAction.UPDATE)),
                new Rule(HttpMethod.PATCH, pattern, req(menuPath, PermissionAction.UPDATE)),
                new Rule(HttpMethod.DELETE, pattern, req(menuPath, PermissionAction.DELETE)));
    }

    private static List<Requirement> req(String menuPath, PermissionAction action) {
        return List.of(new Requirement(menuPath, action));
    }

    private static List<Requirement> anyOf(Requirement... requirements) {
        return List.of(requirements);
    }
}
