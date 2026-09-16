package com.step.dictionary.menu;

import com.step.dictionary.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 사용자별 메뉴 권한. "이 사용자에게 이 메뉴를 보여준다"는 한 줄이다.
 *
 * <p>테이블: {@code app_user_menu} (V10 마이그레이션). 사용자-메뉴 조합은 유일하다.
 * 권한 행이 하나도 없는 사용자는 기본 규칙(사용 중 + 역할)대로 메뉴를 본다 ({@link MenuPermissionService}).
 *
 * <p>만들거나 지우기만 하고 수정하지 않으므로 {@code BaseTimeEntity}를 상속하지 않고 부여 일시만 둔다.
 */
@Entity
@Table(name = "app_user_menu")
@EntityListeners(AuditingEntityListener.class)
public class UserMenu {

    /** 메뉴 권한 ID (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 권한을 받은 사용자. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** 보여줄 메뉴 (하위 메뉴). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    /** 부여 일시. {@code JpaConfig}의 감사 설정이 채운다. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** JPA 전용 기본 생성자. */
    protected UserMenu() {
    }

    public UserMenu(AppUser user, Menu menu) {
        this.user = user;
        this.menu = menu;
    }

    // ---- 조회용 getter ----

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
