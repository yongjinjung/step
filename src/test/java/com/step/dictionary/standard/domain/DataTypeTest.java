package com.step.dictionary.standard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataTypeTest {

    @Test
    @DisplayName("문자 타입은 길이가 필수이고 최대 길이를 넘을 수 없으며 소수점 자리수를 쓸 수 없다")
    void characterSpec() {
        assertThat(DataType.VARCHAR.isValidSpec(20, null)).isTrue();
        assertThat(DataType.VARCHAR.isValidSpec(null, null)).isFalse();
        assertThat(DataType.VARCHAR.isValidSpec(0, null)).isFalse();
        assertThat(DataType.VARCHAR.isValidSpec(20, 2)).isFalse();
        assertThat(DataType.CHAR.isValidSpec(255, null)).isTrue();
        assertThat(DataType.CHAR.isValidSpec(256, null)).isFalse();
    }

    @Test
    @DisplayName("DECIMAL의 소수점 자리수는 생략할 수 있고 길이와 30을 넘을 수 없다")
    void decimalSpec() {
        assertThat(DataType.DECIMAL.isValidSpec(15, 2)).isTrue();
        assertThat(DataType.DECIMAL.isValidSpec(15, null)).isTrue();
        assertThat(DataType.DECIMAL.isValidSpec(10, 11)).isFalse();
        assertThat(DataType.DECIMAL.isValidSpec(40, 31)).isFalse();
        assertThat(DataType.DECIMAL.isValidSpec(66, 0)).isFalse();
    }

    @Test
    @DisplayName("길이를 쓰지 않는 타입은 길이와 소수점 자리수를 입력할 수 없다")
    void noLengthSpec() {
        assertThat(DataType.DATE.isValidSpec(null, null)).isTrue();
        assertThat(DataType.DATE.isValidSpec(8, null)).isFalse();
        assertThat(DataType.BIGINT.isValidSpec(null, 0)).isFalse();
    }

    @Test
    @DisplayName("DECIMAL의 소수점 자리수를 생략하면 0으로 정규화한다")
    void normalizeScale() {
        assertThat(DataType.DECIMAL.normalizeScale(null)).isZero();
        assertThat(DataType.DECIMAL.normalizeScale(2)).isEqualTo(2);
        assertThat(DataType.VARCHAR.normalizeScale(null)).isNull();
    }
}
