package com.step.dictionary.menu;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 메뉴 생성기.
 *
 * <p>메뉴가 하나도 없으면 아무 화면에도 갈 수 없으므로, 기동이 끝나면({@link ApplicationRunner})
 * 메뉴 테이블이 비어 있는지 확인하고 비어 있으면 기본 구성을 만든다. 이미 메뉴가 있으면 아무것도 하지 않으므로
 * 관리자가 화면에서 고친 메뉴는 그대로 유지된다.
 *
 * <p>마이그레이션(V9)에 INSERT로 넣지 않는 이유: 테스트는 매 통합 테스트마다 모든 테이블을 비우므로
 * 마이그레이션으로 넣은 행은 곧 사라져 의미가 없고, 새로 만든 DB에서만 필요한 값이라서다.
 */
@Component
public class MenuInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MenuInitializer.class);

    /**
     * 기본 메뉴 구성. 화면(프론트엔드)에 이미 있는 화면들을 상위 메뉴로 묶은 것이다.
     * 아이콘 이름은 화면이 아는 이름이어야 한다 (frontend {@code layouts/menuIcons.tsx}).
     */
    private static final List<TopMenu> DEFAULT_MENUS = List.of(
            new TopMenu("표준 사전", "book", false, List.of(
                    new SubMenu("표준 단어", "/words", "book", false),
                    new SubMenu("표준 도메인", "/domains", "cluster", false),
                    new SubMenu("표준 용어", "/terms", "apartment", false),
                    new SubMenu("공통 코드", "/codes", "tags", false))),
            new TopMenu("메타 관리", "table", false, List.of(
                    new SubMenu("테이블 정의", "/tables", "table", false),
                    new SubMenu("비표준 컬럼", "/non-standard-columns", "file-search", false))),
            new TopMenu("업무", "audit", false, List.of(
                    new SubMenu("신청/승인", "/requests", "audit", false),
                    new SubMenu("변경 이력", "/histories", "history", false))),
            new TopMenu("관리", "setting", true, List.of(
                    new SubMenu("사용자 관리", "/users", "team", true),
                    new SubMenu("메뉴 관리", "/menus", "menu", true),
                    new SubMenu("메뉴 권한", "/menu-permissions", "lock", true))));

    /** 기본 상위 메뉴 하나와 그 하위 메뉴들. */
    private record TopMenu(String name, String icon, boolean adminOnly, List<SubMenu> children) {
    }

    /** 기본 하위 메뉴 하나. */
    private record SubMenu(String name, String path, String icon, boolean adminOnly) {
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
            Menu parent = menuRepository.save(new Menu(null, top.name(), null, top.icon(), ++topOrder, true,
                    top.adminOnly()));
            int childOrder = 0;
            for (SubMenu child : top.children()) {
                menuRepository.save(new Menu(parent, child.name(), child.path(), child.icon(), ++childOrder, true,
                        child.adminOnly()));
            }
        }
        log.info("기본 메뉴를 만들었습니다: 상위 {}개", DEFAULT_MENUS.size());
    }
}
