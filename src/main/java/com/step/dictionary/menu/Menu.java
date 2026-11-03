package com.step.dictionary.menu;

import com.step.dictionary.global.entity.BaseTimeEntity;
import org.springframework.util.StringUtils;

/**
 * 화면 메뉴. 위쪽(TOP) 상위 메뉴와 그 아래 왼쪽(LEFT) 하위 메뉴를 2단으로 담는다.
 *
 * <p>테이블: {@code app_menu} (V9 마이그레이션).
 * <ul>
 *   <li>상위 메뉴: {@code parent}가 없고 경로도 없다. 화면이 아니라 하위 메뉴를 묶는 이름이다.</li>
 *   <li>하위 메뉴: 상위 메뉴에 속하며 경로({@code path})가 실제 화면을 가리킨다. 경로는 전체에서 유일하다.</li>
 * </ul>
 * 3단 이상은 두지 않는다. 규칙 검사는 {@link MenuService}에서 한다.
 *
 * <p>누가 이 메뉴를 볼 수 있고 무엇을 할 수 있는지는 역할의 메뉴 권한({@code RoleMenu})이 정한다 (12단계).
 * 화면 경로는 주소이면서 권한의 기준이기도 해서({@code MenuPaths}), 쓰고 있는 메뉴의 경로는 바꾸지 않는 것이 좋다.
 */
public class Menu extends BaseTimeEntity {

    /** 메뉴 ID (AUTO_INCREMENT). */
    private Long id;

    /** 상위 메뉴. null이면 자기 자신이 상위(TOP) 메뉴다. */
    private Menu parent;

    /** 메뉴명. 예: 표준 단어 */
    private String name;

    /** 화면 경로 (하위 메뉴만). 예: /words. 상위 메뉴는 null이다. */
    private String path;

    /** 아이콘 이름 (선택). 화면이 아이콘 컴포넌트로 바꾼다. 예: book */
    private String icon;

    /** 같은 단계 안에서의 정렬 순서. 같으면 ID 순으로 정렬한다. */
    private int sortOrder;

    /** 사용 여부 ('Y'/'N'). 미사용 메뉴는 화면 메뉴에 나오지 않는다. */
    private boolean used;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected Menu() {
    }

    /**
     * 새 메뉴를 만든다. 값 규칙은 {@link #update}와 같다.
     *
     * @param parent    상위 메뉴 (null이면 상위 메뉴 자신)
     * @param name      메뉴명
     * @param path      화면 경로 (상위 메뉴는 null)
     * @param icon      아이콘 이름 (null 가능)
     * @param sortOrder 정렬 순서
     * @param used      사용 여부
     */
    public Menu(Menu parent, String name, String path, String icon, int sortOrder, boolean used) {
        update(parent, name, path, icon, sortOrder, used);
    }

    /**
     * 경로를 정리한다. 빈 값은 null(상위 메뉴)로 두고, '/'로 시작하지 않으면 붙인다.
     * 중복 검사도 이 값으로 해야 '/words'와 'words'를 같은 경로로 판단한다.
     */
    public static String normalizePath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String trimmed = path.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    /** 모든 값을 교체한다 (PUT 의미). 상위 메뉴도 바꿀 수 있다(다른 묶음으로 옮기기). */
    public void update(Menu parent, String name, String path, String icon, int sortOrder, boolean used) {
        this.parent = parent;
        this.name = name;
        this.path = normalizePath(path);
        this.icon = StringUtils.hasText(icon) ? icon.trim() : null;
        this.sortOrder = sortOrder;
        this.used = used;
    }

    /** 상위(TOP) 메뉴인지 (상위 메뉴가 없으면 상위). */
    public boolean isTop() {
        return parent == null;
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public Menu getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getIcon() {
        return icon;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isUsed() {
        return used;
    }
}
