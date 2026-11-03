package com.step.dictionary.pharmacy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 약제 수량(투여량·처방량·조제/불출/반납 수량)의 자릿수 규칙.
 *
 * <p>DB 컬럼은 {@code DECIMAL(12,3)}이라 다시 읽으면 항상 소수 3자리(예: 21.000)다. 저장 직후 응답도 같은 모양이 되도록
 * 엔티티에 넣을 때 소수 3자리로 맞춘다. 요청은 {@code @Digits(fraction = 3)}로 검증하므로 반올림이 일어나지 않는다.
 */
public final class Quantities {

    /** 소수 자릿수. 컬럼 정의(DECIMAL(12,3))와 같아야 한다. */
    public static final int SCALE = 3;

    /** 0 (소수 3자리). 반납이 없는 품목의 반납 수량으로 쓴다. */
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    /** 인스턴스 생성 방지. */
    private Quantities() {
    }

    /** 소수 3자리로 맞춘다. null은 그대로 둔다. */
    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
