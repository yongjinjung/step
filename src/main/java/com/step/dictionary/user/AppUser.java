package com.step.dictionary.user;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.role.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.hibernate.annotations.BatchSize;
import org.hibernate.type.YesNoConverter;

/**
 * 사용자 계정.
 *
 * <p>테이블: {@code app_user} (V5 마이그레이션). 클래스 이름을 {@code User}로 하지 않은 이유는
 * Spring Security의 {@code org.springframework.security.core.userdetails.User}와 헷갈리지 않게 하기 위해서다.
 *
 * <p>규칙:
 * <ul>
 *   <li>아이디는 소문자로 저장하며 생성 후 바꿀 수 없다. JWT subject로 쓰이기 때문이다.</li>
 *   <li>비밀번호는 해시만 저장한다. 엔티티는 해시된 값을 받기만 하고 인코딩은 서비스가 {@code PasswordEncoder}로 한다.</li>
 *   <li>삭제하지 않고 비활성화한다. 비활성 계정은 로그인할 수 없다.</li>
 *   <li>역할은 여러 개 가질 수 있고(12단계), 권한은 역할들의 합집합이다.</li>
 * </ul>
 */
@Entity
@Table(name = "app_user")
public class AppUser extends BaseTimeEntity {

    /** 사용자 ID (AUTO_INCREMENT). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디 (소문자). 변경 불가. */
    @Column(nullable = false, length = 50, updatable = false)
    private String username;

    /** 비밀번호 해시 ({bcrypt} 접두어 포함). 응답에 절대 포함하지 않는다. */
    @Column(nullable = false, length = 100)
    private String password;

    /** 표시 이름. */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 부여된 역할 (V11의 {@code app_user_role}). 권한은 역할들의 합집합이다.
     * 목록 조회에서 사용자마다 역할을 따로 조회하지 않도록 {@code @BatchSize}로 묶어 읽는다.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "app_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @BatchSize(size = 50)
    private Set<Role> roles = new LinkedHashSet<>();

    /** 활성 여부 ('Y'/'N'). false면 로그인할 수 없다. */
    @Convert(converter = YesNoConverter.class)
    @Column(nullable = false, length = 1)
    private boolean enabled;

    /** JPA 전용 기본 생성자. */
    protected AppUser() {
    }

    /**
     * 새 계정을 만든다. 생성 직후에는 활성 상태다.
     *
     * @param username        아이디 (소문자로 정규화)
     * @param encodedPassword {@code PasswordEncoder}로 인코딩한 비밀번호
     * @param name            표시 이름
     * @param roles           부여할 역할 (하나 이상, 검증은 서비스에서)
     */
    public AppUser(String username, String encodedPassword, String name, Collection<Role> roles) {
        this.username = normalizeUsername(username);
        this.password = encodedPassword;
        this.name = name;
        this.roles = new LinkedHashSet<>(roles);
        this.enabled = true;
    }

    /** 아이디는 소문자로 저장한다. 중복 검사와 로그인 조회 전에도 같은 규칙을 적용해야 한다. */
    public static String normalizeUsername(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    /**
     * 이름, 역할, 활성 여부를 교체한다 (관리자 수정). 아이디와 비밀번호는 바꾸지 않는다.
     * 마지막 전권 사용자 보호 규칙은 호출 전에 서비스에서 검사해야 한다.
     * 역할 목록은 {@code clear()} 후 다시 담아 기존 연결을 정리한다.
     */
    public void update(String name, Collection<Role> roles, boolean enabled) {
        this.name = name;
        this.roles.clear();
        this.roles.addAll(roles);
        this.enabled = enabled;
    }

    /** 비밀번호를 바꾼다. 인코딩된 값을 받는다. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 전권 역할(사용 중인 것)을 가졌는지. 마지막 전권 사용자 보호 규칙에서 쓴다. */
    public boolean hasSuperAdminRole() {
        return roles.stream().anyMatch(role -> role.isSuperAdmin() && role.isUsed());
    }

    // ---- 조회용 getter ----

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    /** 부여된 역할. 변경은 {@link #update}로만 한다. */
    public Set<Role> getRoles() {
        return Set.copyOf(roles);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
