package com.step.dictionary.user;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 최초 관리자 계정 설정 ({@code app.initial-admin.*}).
 * {@link InitialAdminInitializer}가 사용자 테이블이 비어 있을 때 한 번만 이 값으로 관리자를 만든다.
 *
 * @param username 관리자 아이디. 기본 {@code admin}
 * @param password 관리자 초기 비밀번호. 비어 있으면 계정을 만들지 않는다.
 *                 운영에서는 환경변수 {@code INITIAL_ADMIN_PASSWORD}로 넣고, 첫 로그인 후 바로 바꾼다
 * @param name     관리자 표시 이름. 기본 {@code 관리자}
 */
@ConfigurationProperties("app.initial-admin")
public record InitialAdminProperties(
        @DefaultValue("admin") String username,
        String password,
        @DefaultValue("관리자") String name
) {
}
