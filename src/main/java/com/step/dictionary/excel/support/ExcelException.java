package com.step.dictionary.excel.support;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.error.ErrorResponse.FieldErrorDetail;
import java.util.List;

/**
 * 엑셀 파일/행 오류. {@link BusinessException}에 행별 상세 사유를 더한다.
 * {@code GlobalExceptionHandler}가 상세 사유를 응답의 fieldErrors로 내려준다.
 */
public class ExcelException extends BusinessException {

    /** 상세 사유 (예: {@code "3행 영문약어"} → 메시지). 없으면 빈 목록. */
    private final List<FieldErrorDetail> details;

    public ExcelException(ErrorCode errorCode, List<FieldErrorDetail> details) {
        super(errorCode);
        this.details = List.copyOf(details);
    }

    public ExcelException(ErrorCode errorCode) {
        this(errorCode, List.of());
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }
}
