package com.step.dictionary.user;

import com.step.dictionary.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    // MySQL 방언은 STRING enum을 ENUM 타입으로 보므로 VARCHAR 컬럼과 validate가 어긋나지 않게 지정한다.
    /** 역할. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Role role;

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
     * @param role            역할
     */
    public AppUser(String username, String encodedPassword, String name, Role role) {
        this.username = normalizeUsername(username);
        this.password = encodedPassword;
        this.name = name;
        this.role = role;
        this.enabled = true;
    }

    /** 아이디는 소문자로 저장한다. 중복 검사와 로그인 조회 전에도 같은 규칙을 적용해야 한다. */
    public static String normalizeUsername(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    /**
     * 이름, 역할, 활성 여부를 교체한다 (관리자 수정). 아이디와 비밀번호는 바꾸지 않는다.
     * 마지막 활성 관리자 보호 규칙은 호출 전에 서비스에서 검사해야 한다.
     */
    public void update(String name, Role role, boolean enabled) {
        this.name = name;
        this.role = role;
        this.enabled = enabled;
    }

    /** 비밀번호를 바꾼다. 인코딩된 값을 받는다. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /** 활성 상태의 관리자인지. 마지막 관리자 보호 규칙에서 쓴다. */
    public boolean isActiveAdmin() {
        return enabled && role == Role.ADMIN;
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

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
