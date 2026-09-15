package com.step.dictionary.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 최초 관리자 계정 생성기.
 *
 * <p>공개 회원가입이 없으므로, 사용자가 한 명도 없으면 아무도 로그인할 수 없다.
 * 애플리케이션 기동이 끝나면({@link ApplicationRunner}) 사용자 테이블이 비어 있는지 확인하고,
 * 비어 있으면 {@link InitialAdminProperties} 값으로 ADMIN 계정을 하나 만든다.
 * 사용자가 이미 있으면 아무것도 하지 않으므로 설정 비밀번호를 바꿔도 기존 계정에는 영향이 없다.
 */
@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitialAdminProperties properties;

    public InitialAdminInitializer(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            InitialAdminProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    /** 기동 직후 한 번 호출된다. */
    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        // 비밀번호를 기본값으로 두면 누구나 아는 비밀번호의 관리자가 생기므로, 설정이 없으면 만들지 않는다.
        if (!StringUtils.hasText(properties.password())) {
            log.warn("사용자가 없지만 app.initial-admin.password가 설정되지 않아 최초 관리자를 만들지 않았습니다.");
            return;
        }

        AppUser admin = userRepository.save(new AppUser(
                properties.username(), passwordEncoder.encode(properties.password()), properties.name(), Role.ADMIN));
        log.info("최초 관리자 계정을 만들었습니다: {}", admin.getUsername());
    }
}
