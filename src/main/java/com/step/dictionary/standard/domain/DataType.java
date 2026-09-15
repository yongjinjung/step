package com.step.dictionary.standard.domain;

/**
 * 도메인 데이터 타입. 타입마다 길이/소수점 자리수 사용 여부와 최대값이 다르다 (MySQL 기준).
 *
 * <p>도메인({@link Domain})과 컬럼 정의({@code MetaColumn})가 같은 규칙을 쓴다.
 * DB에는 enum 이름 문자열(VARCHAR)로 저장한다.
 *
 * <table>
 *   <tr><th>타입</th><th>길이</th><th>소수점</th></tr>
 *   <tr><td>CHAR</td><td>1~255 필수</td><td>불가</td></tr>
 *   <tr><td>VARCHAR</td><td>1~65535 필수</td><td>불가</td></tr>
 *   <tr><td>DECIMAL</td><td>1~65 필수</td><td>0~min(길이, 30), 생략 시 0</td></tr>
 *   <tr><td>INTEGER, BIGINT, DATE, DATETIME, TEXT</td><td>불가</td><td>불가</td></tr>
 * </table>
 */
public enum DataType {

    /** 고정 길이 문자열. */
    CHAR(255, false),
    /** 가변 길이 문자열. */
    VARCHAR(65535, false),
    /** 고정 소수점 숫자. 길이(전체 자리수)와 소수점 자리수를 쓴다. */
    DECIMAL(65, true),
    /** 32비트 정수. */
    INTEGER,
    /** 64비트 정수. */
    BIGINT,
    /** 날짜. */
    DATE,
    /** 날짜 + 시각. */
    DATETIME,
    /** 긴 문자열. */
    TEXT;

    /** MySQL DECIMAL의 최대 소수점 자리수. */
    private static final int MAX_DECIMAL_SCALE = 30;

    /** 최대 길이. 0이면 길이를 쓰지 않는 타입이다. */
    private final int maxLength;
    /** 소수점 자리수를 쓸 수 있는지 여부 (DECIMAL만 true). */
    private final boolean scalable;

    /** 길이와 소수점을 모두 쓰지 않는 타입용 생성자. */
    DataType() {
        this(0, false);
    }

    DataType(int maxLength, boolean scalable) {
        this.maxLength = maxLength;
        this.scalable = scalable;
    }

    /**
     * 길이와 소수점 자리수 조합이 이 타입에 맞는지 검사한다.
     * 길이를 쓰는 타입은 길이가 필수이고, 소수점 자리수는 DECIMAL만 쓸 수 있다.
     *
     * @param length 길이 (null 가능)
     * @param scale  소수점 자리수 (null 가능)
     * @return 규칙에 맞으면 true
     */
    public boolean isValidSpec(Integer length, Integer scale) {
        // 길이를 쓰지 않는 타입: 둘 다 비어 있어야 한다.
        if (maxLength == 0) {
            return length == null && scale == null;
        }
        // 길이를 쓰는 타입: 길이는 1 이상 최대값 이하로 필수다.
        if (length == null || length < 1 || length > maxLength) {
            return false;
        }
        // 소수점 생략은 모든 길이 타입에서 허용한다 (DECIMAL은 저장 시 0으로 정규화).
        if (scale == null) {
            return true;
        }
        // 소수점을 지정했다면 DECIMAL이어야 하고, 0 이상이면서 길이와 30을 넘지 않아야 한다.
        return scalable && scale >= 0 && scale <= length && scale <= MAX_DECIMAL_SCALE;
    }

    /**
     * DECIMAL의 소수점 자리수를 생략하면 0으로 저장한다 (MySQL {@code DECIMAL(M)}과 같다).
     * 나머지 타입은 입력값을 그대로 돌려준다. 도메인과 컬럼이 같은 규칙을 써야 비표준 비교가 어긋나지 않는다.
     */
    public Integer normalizeScale(Integer scale) {
        // 0을 Integer로 두지 않으면 삼항 연산 결과가 int로 언박싱되어 scale이 null일 때 NPE가 난다.
        return scalable && scale == null ? Integer.valueOf(0) : scale;
    }
}
