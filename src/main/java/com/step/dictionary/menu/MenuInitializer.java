package com.step.dictionary.menu;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 메뉴 생성기.
 *
 * <p>메뉴가 하나도 없으면 아무 화면에도 갈 수 없으므로, 기동이 끝나면({@link ApplicationRunner})
 * 메뉴 테이블이 비어 있는지 확인하고 비어 있으면 기본 구성을 만든다. 이미 메뉴가 있으면 아무것도 하지 않으므로
 * 관리자가 화면에서 고친 메뉴는 그대로 유지된다.
 *
 * <p>누가 이 메뉴를 볼 수 있는지는 역할의 메뉴 권한이 정한다. 관리자 역할은 전권이라 모두 보이고,
 * 일반 사용자 역할의 기본 권한은 {@code RoleInitializer}가 채운다.
 *
 * <p>마이그레이션에 INSERT로 넣지 않는 이유: 테스트는 매 통합 테스트마다 모든 테이블을 비우므로
 * 마이그레이션으로 넣은 행은 곧 사라져 의미가 없고, 새로 만든 DB에서만 필요한 값이라서다.
 */
@Component
@Order(MenuInitializer.ORDER)
public class MenuInitializer implements ApplicationRunner {

    /**
     * 실행 순서. 메뉴 권한을 채우는 {@code RoleInitializer}보다 먼저 실행돼야 한다.
     * {@code @Order}가 없으면 가장 나중({@code LOWEST_PRECEDENCE})에 실행되어, 새 DB에서 처음 기동할 때
     * 일반 사용자 역할의 기본 권한이 하나도 만들어지지 않는다.
     */
    public static final int ORDER = 0;

    private static final Logger log = LoggerFactory.getLogger(MenuInitializer.class);

    /**
     * 기본 메뉴 구성. 화면(프론트엔드)에 이미 있는 화면들을 상위 메뉴로 묶은 것이다.
     * 아이콘 이름은 화면이 아는 이름이어야 한다 (frontend {@code layouts/menuIcons.tsx}).
     * 경로는 권한의 기준이기도 하므로({@code MenuPaths}) 화면 경로와 같아야 한다.
     */
    private static final List<TopMenu> DEFAULT_MENUS = List.of(
            new TopMenu("표준 사전", "book", List.of(
                    new SubMenu("표준 단어", "/words", "book"),
                    new SubMenu("표준 도메인", "/domains", "cluster"),
                    new SubMenu("표준 용어", "/terms", "apartment"),
                    new SubMenu("공통 코드", "/codes", "tags"))),
            new TopMenu("메타 관리", "table", List.of(
                    new SubMenu("테이블 정의", "/tables", "table"),
                    new SubMenu("비표준 컬럼", "/non-standard-columns", "file-search"))),
            new TopMenu("업무", "audit", List.of(
                    new SubMenu("신청/승인", "/requests", "audit"),
                    new SubMenu("변경 이력", "/histories", "history"))),
            // 약제 업무(13단계): 처방 → 조제 → 검수 → 불출 → 반납 순서대로 둔다.
            new TopMenu("약제", "database", List.of(
                    new SubMenu("처방", "/prescriptions", "file"),
                    new SubMenu("조제", "/dispensings", "appstore"),
                    new SubMenu("검수", "/inspections", "file-search"),
                    new SubMenu("불출", "/issues", "folder"),
                    new SubMenu("반납", "/returns", "history"))),
            new TopMenu("관리", "setting", List.of(
                    new SubMenu("사용자 관리", "/users", "team"),
                    new SubMenu("역할 관리", "/roles", "safety"),
                    new SubMenu("메뉴 관리", "/menus", "menu"),
                    new SubMenu("메뉴 권한", "/menu-permissions", "lock"))),
            // 초급 개발자 온보딩용 참고 화면. 업무 데이터를 다루지 않아 RoleInitializer가 USER 역할에도 기본으로 열어 준다.
            new TopMenu("샘플", "book", List.of(
                    new SubMenu("React", "/react", "book"),
                    new SubMenu("TypeScript", "/typescript", "book"),
                    new SubMenu("vite", "/vite", "book"),
                    new SubMenu("Ant Design 6", "/antdesign", "book"),
                    new SubMenu("라우팅과 탭", "/router", "cluster"),
                    new SubMenu("API 연동", "/data-fetching", "database"),
                    new SubMenu("폼과 검증", "/forms", "form"),
                    new SubMenu("서버 개발 가이드", "/backend-guide", "appstore"))));

    /** 기본 상위 메뉴 하나와 그 하위 메뉴들. */
    private record TopMenu(String name, String icon, List<SubMenu> children) {
    }

    /** 기본 하위 메뉴 하나. */
    private record SubMenu(String name, String path, String icon) {
    }

    private final MenuRepository menuRepository;

    public MenuInitializer(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    /** 기동 직후 한 번 호출된다. 상위 메뉴를 먼저 저장해야 하위 메뉴가 참조할 ID가 생긴다. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (menuRepository.count() > 0) {
            return;
        }

        int topOrder = 0;
        for (TopMenu top : DEFAULT_MENUS) {
            Menu parent = menuRepository.save(new Menu(null, top.name(), null, top.icon(), ++topOrder, true));
            int childOrder = 0;
            for (SubMenu child : top.children()) {
                menuRepository.save(new Menu(parent, child.name(), child.path(), child.icon(), ++childOrder, true));
            }
        }
        log.info("기본 메뉴를 만들었습니다: 상위 {}개", DEFAULT_MENUS.size());
    }
}
