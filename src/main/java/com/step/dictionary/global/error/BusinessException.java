package com.step.dictionary.global.error;

/**
 * 업무 규칙 위반을 나타내는 예외.
 *
 * <p>서비스는 검증에 실패하면 이 예외를 {@link ErrorCode}와 함께 던진다.
 * {@link GlobalExceptionHandler}가 받아 ErrorCode의 HTTP 상태와 {@code {code, message, fieldErrors}} 형식으로 응답한다.
 * 런타임 예외이므로 {@code @Transactional} 메서드에서 던지면 트랜잭션이 롤백된다.
 */
public class BusinessException extends RuntimeException {

    /** 응답 코드, HTTP 상태, 메시지를 담은 오류 종류. */
    private final ErrorCode errorCode;

    /** 예외 메시지는 ErrorCode의 기본 메시지를 그대로 쓴다. */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
