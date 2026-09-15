package com.step.dictionary.standard.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.standard.word.Word;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainTest {

    private final Word amount = new Word("금액", "AMT", "Amount", null, true);
    private final Word number = new Word("번호", "NO", "Number", null, true);

    @Test
    @DisplayName("DECIMAL 도메인의 소수점 자리수를 생략하면 0으로 저장된다")
    void normalizeScaleOnCreate() {
        Domain domain = new Domain("금액N15", amount, DataType.DECIMAL, 15, null, null);

        assertThat(domain.getDataScale()).isZero();
    }

    @Test
    @DisplayName("수정하면 분류어와 타입 규칙이 모두 바뀐다")
    void update() {
        Domain domain = new Domain("금액N15", amount, DataType.DECIMAL, 15, 2, null);

        domain.update("번호V20", number, DataType.VARCHAR, 20, null, "식별 번호");

        assertThat(domain.getName()).isEqualTo("번호V20");
        assertThat(domain.getClassifierWord()).isSameAs(number);
        assertThat(domain.getDataType()).isEqualTo(DataType.VARCHAR);
        assertThat(domain.getDataLength()).isEqualTo(20);
        assertThat(domain.getDataScale()).isNull();
        assertThat(domain.getDescription()).isEqualTo("식별 번호");
    }
}
