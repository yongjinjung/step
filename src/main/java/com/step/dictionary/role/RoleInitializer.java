package com.step.dictionary.role;

import com.step.dictionary.menu.Menu;
import com.step.dictionary.menu.MenuRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기본 역할 권한 생성기.
 *
 * <p>ADMIN 역할은 전권이라 메뉴 권한이 필요 없지만, USER 역할은 메뉴별 권한이 있어야 아무 화면도 볼 수 없다.
 * 메뉴 권한은 메뉴가 만들어진 뒤에야 줄 수 있어 마이그레이션(V11)이 아니라 기동 시 채운다.
 * 이미 권한이 하나라도 있으면 아무것도 하지 않으므로 관리자가 화면에서 고친 권한은 유지된다.
 *
 * <p>{@code MenuInitializer}가 먼저 실행돼야 메뉴를 찾을 수 있어 실행 순서를 지정한다.
 */
@Component
@Order(RoleInitializer.ORDER)
public class RoleInitializer implements ApplicationRunner {

    /** 메뉴 생성기(기본값 0) 다음에 실행한다. */
    public static final int ORDER = 100;

    private static final Logger log = LoggerFactory.getLogger(RoleInitializer.class);

    /**
     * 일반 사용자 역할의 기본 권한. 예전 USER 역할이 할 수 있던 일을 그대로 옮긴 것이다.
     * (표준 데이터는 조회만 하고 변경은 신청으로, 테이블/컬럼 정의는 직접 관리)
     */
    // Map.of는 항목 10개까지만 받으므로(샘플 화면 6개를 더하며 넘었다) Map.ofEntries로 바꾼다.
    private static final Map<String, Set<PermissionAction>> DEFAULT_USER_PERMISSIONS = Map.ofEntries(
            Map.entry("/words", Set.of(PermissionAction.READ)),
            Map.entry("/domains", Set.of(PermissionAction.READ)),
            Map.entry("/terms", Set.of(PermissionAction.READ)),
            Map.entry("/codes", Set.of(PermissionAction.READ)),
            Map.entry("/tables", Set.of(PermissionAction.READ, PermissionAction.CREATE, PermissionAction.UPDATE,
                    PermissionAction.DELETE)),
            Map.entry("/non-standard-columns", Set.of(PermissionAction.READ)),
            Map.entry("/requests", Set.of(PermissionAction.READ, PermissionAction.CREATE)),
            Map.entry("/histories", Set.of(PermissionAction.READ)),
            // 약제 업무는 조회만 연다. 처방·조제 등을 등록할 사람(약사 등)에게는 관리자가 역할을 따로 만들어 준다.
            Map.entry("/prescriptions", Set.of(PermissionAction.READ)),
            Map.entry("/dispensings", Set.of(PermissionAction.READ)),
            Map.entry("/inspections", Set.of(PermissionAction.READ)),
            Map.entry("/issues", Set.of(PermissionAction.READ)),
            Map.entry("/returns", Set.of(PermissionAction.READ)),
            // 샘플 화면은 업무 데이터를 다루지 않는 온보딩 참고 자료라 누구나 볼 수 있게 조회만 연다.
            Map.entry("/react", Set.of(PermissionAction.READ)),
            Map.entry("/typescript", Set.of(PermissionAction.READ)),
            Map.entry("/vite", Set.of(PermissionAction.READ)),
            Map.entry("/antdesign", Set.of(PermissionAction.READ)),
            Map.entry("/router", Set.of(PermissionAction.READ)),
            Map.entry("/data-fetching", Set.of(PermissionAction.READ)),
            Map.entry("/forms", Set.of(PermissionAction.READ)),
            Map.entry("/backend-guide", Set.of(PermissionAction.READ)));

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;

    public RoleInitializer(RoleRepository roleRepository, RoleMenuRepository roleMenuRepository,
            MenuRepository menuRepository) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
    }

    /** 기동 직후 한 번 호출된다. */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (roleMenuRepository.count() > 0) {
            return;
        }
        Optional<Role> userRole = roleRepository.findByCode(Role.USER_CODE);
        if (userRole.isEmpty()) {
            return;
        }

        List<Menu> menus = menuRepository.findAllByOrderBySortOrderAscIdAsc();
        List<RoleMenu> permissions = menus.stream()
                .filter(menu -> menu.getPath() != null && DEFAULT_USER_PERMISSIONS.containsKey(menu.getPath()))
                .map(menu -> {
                    Set<PermissionAction> actions = DEFAULT_USER_PERMISSIONS.get(menu.getPath());
                    return new RoleMenu(userRole.get(), menu,
                            actions.contains(PermissionAction.READ),
                            actions.contains(PermissionAction.CREATE),
                            actions.contains(PermissionAction.UPDATE),
                            actions.contains(PermissionAction.DELETE));
                })
                .toList();
        if (permissions.isEmpty()) {
            return;
        }

        roleMenuRepository.saveAll(permissions);
        log.info("일반 사용자 역할의 기본 메뉴 권한 {}건을 만들었습니다.", permissions.size());
    }
}
