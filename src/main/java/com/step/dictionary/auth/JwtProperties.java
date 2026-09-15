package com.step.dictionary.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * JWT 설정 ({@code app.jwt.*}).
 *
 * <p>토큰은 HS256(HMAC-SHA256) 대칭키로 서명한다. 발급과 검증을 이 서버가 모두 하므로 공개키 방식이 필요 없다.
 * 비밀키가 없거나 짧으면 기동 시점에 실패시켜, 약한 키로 운영되는 일을 막는다.
 *
 * @param secret     서명 비밀키. UTF-8 기준 32바이트(256비트) 이상. 운영에서는 환경변수 {@code JWT_SECRET}으로 넣는다
 * @param expiration 액세스 토큰 유효 시간. 기본 1시간. 역할 변경/비활성화가 반영되기까지 걸리는 최대 시간이기도 하다
 * @param issuer     발급자({@code iss} 클레임). 검증 시 이 값과 같아야 한다. 기본 {@code dictionary}
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("1h") Duration expiration,
        @DefaultValue("dictionary") String issuer
) {

    /** HS256 키 최소 길이(바이트). 알고리즘 출력 크기(256비트)보다 짧은 키는 쓰지 않는다. */
    static final int MIN_SECRET_BYTES = 32;

    /** 바인딩 직후 값을 검증한다. 실패하면 애플리케이션이 기동하지 않는다. */
    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.jwt.secret은 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다 (환경변수 JWT_SECRET).");
        }
        if (expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("app.jwt.expiration은 0보다 커야 합니다.");
        }
    }

    /** 서명/검증에 쓰는 HMAC-SHA256 키. */
    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
