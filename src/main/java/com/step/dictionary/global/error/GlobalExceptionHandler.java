package com.step.dictionary.global.error;

import com.step.dictionary.excel.support.ExcelException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 컨트롤러에서 발생한 예외를 {@link ErrorResponse} 형식으로 변환한다.
 * 인증/권한 오류(401, 403)는 컨트롤러에 도달하기 전 보안 필터에서 발생하므로 여기가 아니라
 * {@code SecurityErrorHandler}가 같은 형식으로 응답한다.
 * 여기서 처리하지 않는 예외(404 경로 없음, 405 메서드 불일치, 그 밖의 500)는 스프링 부트 기본 오류 응답으로 나간다.
 *
 * <p>같은 예외에 여러 핸들러가 맞으면 스프링은 가장 가까운 타입의 핸들러를 고른다
 * (예: {@link ExcelException}은 {@link BusinessException}의 하위 타입이라 전용 핸들러가 쓰인다).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스가 던진 업무 규칙 위반. ErrorCode에 정의된 HTTP 상태로 응답한다. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    /**
     * 엑셀 업로드 오류. 행별 사유(예: {@code {"field": "3행 영문약어", "reason": "..."}})를 fieldErrors에 담는다.
     */
    @ExceptionHandler(ExcelException.class)
    public ResponseEntity<ErrorResponse> handleExcelException(ExcelException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, e.getDetails()));
    }

    /**
     * 요청 검증 실패(400). 필드별 사유를 fieldErrors에 담는다.
     * {@code @Valid @RequestBody}의 Bean Validation 실패와, 검색 조건 record({@code @ParameterObject})의
     * 쿼리 파라미터 바인딩 실패(예: {@code classifier=abc})가 모두 이 예외로 들어온다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode, e.getBindingResult()));
    }

    /**
     * 필드 단위로 사유를 알 수 없는 잘못된 요청(400).
     * <ul>
     *   <li>{@link HttpMessageNotReadableException}: JSON 문법 오류, 없는 enum 값(예: {@code "dataType": "FOO"}), 본문 누락</li>
     *   <li>{@link MethodArgumentTypeMismatchException}: 경로 변수 타입 불일치 (예: {@code /api/words/abc})</li>
     *   <li>{@link MissingServletRequestPartException}: 업로드 요청에 {@code file} 파트가 없음</li>
     *   <li>{@link MultipartException}: multipart 요청이 아닌데 파일 파라미터를 받는 API를 호출함</li>
     * </ul>
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class, MultipartException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    /**
     * 업로드 파일 크기 초과(413). {@link MultipartException}의 하위 타입이라 위 핸들러보다 이 핸들러가 우선한다.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        ErrorCode errorCode = ErrorCode.EXCEL_FILE_TOO_LARGE;
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}
