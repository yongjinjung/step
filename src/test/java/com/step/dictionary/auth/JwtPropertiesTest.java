package com.step.dictionary.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static final String VALID_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("비밀키가 없거나 32바이트보다 짧으면 설정을 거부한다")
    void rejectShortSecret() {
        assertThatThrownBy(() -> new JwtProperties(null, Duration.ofHours(1), "dictionary"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET.substring(1), Duration.ofHours(1), "dictionary"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.jwt.secret");
    }

    @Test
    @DisplayName("유효 시간이 0 이하면 설정을 거부한다")
    void rejectNonPositiveExpiration() {
        assertThatThrownBy(() -> new JwtProperties(VALID_SECRET, Duration.ZERO, "dictionary"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비밀키로 HMAC-SHA256 키를 만든다")
    void secretKey() {
        JwtProperties properties = new JwtProperties(VALID_SECRET, Duration.ofHours(1), "dictionary");

        assertThat(properties.secretKey().getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(properties.secretKey().getEncoded()).hasSize(32);
    }
}
