package com.step.dictionary.meta;

import java.util.Locale;

/**
 * 테이블/컬럼 물리명 규칙. 용어 영문약어와 비교하므로 대문자로 저장한다.
 *
 * <p>테이블({@code MetaTable})과 컬럼({@code MetaColumn})이 같은 규칙을 공유한다.
 * 상수는 요청 DTO의 검증 어노테이션({@code @Size}, {@code @Pattern})과 엔티티 컬럼 길이에 함께 쓰인다.
 * 인스턴스를 만들지 않는 유틸리티 클래스다.
 */
public final class PhysicalNames {

    /** MySQL 식별자 최대 길이. */
    public static final int MAX_LENGTH = 64;
    /** 영문자로 시작하고 영문/숫자/'_'만 쓰는 형식. 소문자도 허용하며 저장 시 대문자로 바뀐다. */
    public static final String PATTERN = "^[A-Za-z][A-Za-z0-9_]*$";
    /** {@link #PATTERN} 위반 시 fieldErrors에 들어갈 메시지. */
    public static final String PATTERN_MESSAGE = "영문자로 시작하는 영문/숫자/_만 사용할 수 있습니다.";

    /** 인스턴스 생성 방지. */
    private PhysicalNames() {
    }

    /**
     * 물리명을 대문자로 정규화한다. 중복 검사 전에도 같은 규칙을 적용해야 한다.
     * Locale.ROOT를 써서 실행 환경 로케일(예: 터키어)에 따라 결과가 달라지지 않게 한다.
     */
    public static String normalize(String physicalName) {
        return physicalName.toUpperCase(Locale.ROOT);
    }
}
