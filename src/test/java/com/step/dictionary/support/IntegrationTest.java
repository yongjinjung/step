package com.step.dictionary.support;

import com.step.dictionary.auth.JwtTokenProvider;
import com.step.dictionary.menu.MenuInitializer;
import com.step.dictionary.role.Role;
import com.step.dictionary.role.RoleInitializer;
import com.step.dictionary.role.RoleRepository;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * REST Assured 통합 테스트 기반 클래스.
 * 요청은 서버 스레드에서 별도 트랜잭션으로 처리되므로 롤백 대신 매 테스트 전후에 테이블을 비운다.
 * H2 메모리 DB는 모든 테스트 컨텍스트가 공유하므로, 끝날 때도 비워야 뒤이어 실행되는
 * {@code @DataJpaTest}가 남은 데이터와 충돌하지 않는다.
 *
 * <p>권한 준비: 기능 권한은 DB의 역할·메뉴 권한으로 검사하는데(12단계) 테이블을 비우면 그것들도 사라진다.
 * 그래서 매 테스트 전에 마이그레이션이 넣는 기본 역할(ADMIN 전권, USER)을 다시 만들고,
 * 운영과 같은 초기화 코드({@link MenuInitializer}, {@link RoleInitializer})로 기본 메뉴와
 * 일반 사용자 기본 권한을 채운다. 즉 테스트는 "설치 직후" 권한 상태에서 시작한다.
 *
 * <p>인증: {@value #ADMIN_USERNAME}(ADMIN 역할)과 {@value #USER_USERNAME}(USER 역할) 계정을 만들고 토큰을 발급한다.
 * 기본으로 모든 요청에 관리자 토큰을 붙이므로 기능 테스트는 인증을 신경 쓰지 않아도 된다.
 * 다른 권한으로 호출하려면 {@link #authenticate(String)}에 {@link #userToken}이나 null(토큰 없음)을 넘긴다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTest {

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String USER_USERNAME = "user";
    /** 테스트 계정 공통 비밀번호. */
    protected static final String PASSWORD = "password1!";

    /** BCrypt 해시는 계산 비용이 커서 테스트마다 만들지 않고 한 번만 만든다. */
    private static String encodedPassword;

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private MenuInitializer menuInitializer;

    @Autowired
    private RoleInitializer roleInitializer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    /** 매 테스트 전에 발급한 관리자 토큰. */
    protected String adminToken;
    /** 매 테스트 전에 발급한 일반 사용자 토큰. */
    protected String userToken;
    /** 전권 관리자 역할 (계정 생성 API 테스트에서 roleIds로 쓴다). */
    protected Role adminRole;
    /** 일반 사용자 역할. */
    protected Role userRole;

    @BeforeEach
    void setUpIntegrationTest() {
        RestAssured.port = port;
        DatabaseCleaner.clean(jdbcTemplate);
        // 순서: 역할 → 메뉴 → 일반 사용자 기본 권한 (권한은 메뉴를 가리키고, 초기화기는 USER 역할을 찾는다)
        adminRole = roleRepository.save(new Role(Role.ADMIN_CODE, "관리자", "모든 기능", true, true, true));
        userRole = roleRepository.save(new Role(Role.USER_CODE, "일반 사용자", "기본 권한", false, true, true));
        menuInitializer.run(null);
        roleInitializer.run(null);

        adminToken = saveAccountAndIssueToken(ADMIN_USERNAME, adminRole);
        userToken = saveAccountAndIssueToken(USER_USERNAME, userRole);
        authenticate(adminToken);
    }

    @AfterEach
    void tearDownIntegrationTest() {
        RestAssured.requestSpecification = null;
        DatabaseCleaner.clean(jdbcTemplate);
    }

    /**
     * 이후 {@code given()} 요청에 붙일 토큰을 바꾼다.
     *
     * @param token Bearer 토큰. null이면 Authorization 헤더 없이 요청한다
     */
    protected void authenticate(String token) {
        // RequestSpecBuilder는 만들 때 현재 RestAssured.requestSpecification을 기본값으로 복사한다.
        // 먼저 비우지 않으면 이전 토큰 헤더가 새 스펙에 남아 토큰이 바뀌지 않는다.
        RestAssured.requestSpecification = null;
        RestAssured.requestSpecification = token == null
                ? null
                : new RequestSpecBuilder().addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token).build();
    }

    /** 테스트 계정을 저장하고 토큰을 발급한다. 로그인 API를 거치지 않아 빠르다. */
    private String saveAccountAndIssueToken(String username, Role role) {
        if (encodedPassword == null) {
            encodedPassword = passwordEncoder.encode(PASSWORD);
        }
        AppUser user = userRepository.save(new AppUser(username, encodedPassword, username, List.of(role)));
        return tokenProvider.issue(user).accessToken();
    }
}
