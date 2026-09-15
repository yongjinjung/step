package com.step.dictionary.global.error;

import java.util.List;
import org.springframework.validation.BindingResult;

/**
 * 오류 응답 본문. 모든 업무 오류와 입력값 오류는 이 형식으로 내려간다.
 *
 * <pre>
 * {"code": "DUPLICATE_WORD_NAME", "message": "이미 등록된 단어명입니다.", "fieldErrors": []}
 * </pre>
 *
 * @param code        {@link ErrorCode} 이름. 클라이언트는 메시지 대신 이 값으로 분기한다
 * @param message     사람이 읽을 메시지
 * @param fieldErrors 필드 단위 검증 오류 목록. 해당 없으면 빈 배열 (null이 아니다)
 */
public record ErrorResponse(String code, String message, List<FieldErrorDetail> fieldErrors) {

    /**
     * 필드 하나의 검증 오류.
     *
     * @param field  요청 필드명 (예: {@code abbreviation}, {@code wordIds[0]})
     * @param reason 검증 어노테이션의 메시지
     */
    public record FieldErrorDetail(String field, String reason) {
    }

    /** 필드 오류 없이 코드와 메시지만 담는다. */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    /** 이미 만들어진 필드 오류 목록을 담는다. 엑셀 업로드의 행별 오류처럼 BindingResult가 없는 경우에 쓴다. */
    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.copyOf(fieldErrors));
    }

    /** Bean Validation/바인딩 결과의 필드 오류를 함께 담는다. */
    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        List<FieldErrorDetail> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), fieldErrors);
    }
}
