package com.step.dictionary.menu;

import com.step.dictionary.global.entity.CreatedAtAware;
import com.step.dictionary.user.AppUser;
import java.time.LocalDateTime;

/**
 * 사용자별 메뉴 권한. "이 사용자에게 이 메뉴를 보여준다"는 한 줄이다.
 *
 * <p>테이블: {@code app_user_menu} (V10 마이그레이션). 사용자-메뉴 조합은 유일하다.
 * 권한 행이 하나도 없는 사용자는 기본 규칙(사용 중 + 역할)대로 메뉴를 본다 ({@link MenuPermissionService}).
 *
 * <p>만들거나 지우기만 하고 수정하지 않으므로 {@code BaseTimeEntity}를 상속하지 않고 부여 일시만 둔다.
 */
public class UserMenu implements CreatedAtAware {

    /** 메뉴 권한 ID (AUTO_INCREMENT). */
    private Long id;

    /** 권한을 받은 사용자. */
    private AppUser user;

    /** 보여줄 메뉴 (하위 메뉴). */
    private Menu menu;

    /** 부여 일시. {@code JpaConfig}의 감사 설정이 채운다. */
    private LocalDateTime createdAt;

    /** MyBatis가 조회 결과로 객체를 만들 때 쓰는 기본 생성자. */
    protected UserMenu() {
    }

    public UserMenu(AppUser user, Menu menu) {
        this.user = user;
        this.menu = menu;
    }

    // ---- 조회용 getter ----

    @Override
    public void markCreated(LocalDateTime now) {
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public Menu getMenu() {
        return menu;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
