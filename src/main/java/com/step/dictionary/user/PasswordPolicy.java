package com.step.dictionary.user;

/**
 * 비밀번호 입력 규칙. 요청 DTO의 검증 어노테이션에서 함께 쓴다.
 *
 * <p>BCrypt는 입력의 앞 72바이트만 쓰고, Spring Security는 72바이트를 넘으면 예외를 던진다(500).
 * 한글처럼 한 글자가 여러 바이트인 문자가 섞이면 글자 수만으로는 72바이트를 보장할 수 없다.
 * 그래서 공백을 제외한 출력 가능한 ASCII 문자(0x21~0x7E)만 허용하고 최대 64자로 제한한다.
 */
public final class PasswordPolicy {

    /** 최소 길이. */
    public static final int MIN_LENGTH = 8;
    /** 최대 길이. ASCII만 허용하므로 64자 = 64바이트로 BCrypt 한도(72바이트) 안이다. */
    public static final int MAX_LENGTH = 64;
    /** 공백을 제외한 출력 가능한 ASCII 문자('!'~'~'). */
    public static final String PATTERN = "^[!-~]+$";
    /** {@link #PATTERN} 위반 시 fieldErrors에 들어갈 메시지. */
    public static final String PATTERN_MESSAGE = "영문, 숫자, 특수문자만 사용할 수 있습니다 (공백 불가).";

    /** 인스턴스 생성 방지. */
    private PasswordPolicy() {
    }
}
