package com.step.dictionary.excel.support;

import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.error.ErrorResponse.FieldErrorDetail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 업로드 한 번의 행 오류 모음.
 *
 * <p>업로드는 전부 반영하거나 전혀 반영하지 않는다. 그래서 첫 오류에서 멈추지 않고 모든 행을 끝까지 검사해
 * 오류를 모은 뒤, 하나라도 있으면 {@link #throwIfAny()}로 예외를 던져 트랜잭션 전체를 롤백한다.
 * 사용자가 한 번에 모두 고칠 수 있도록 하기 위해서다. 응답이 너무 커지지 않게 앞의 {@value #MAX_ERRORS}건만 담는다.
 */
public final class ExcelErrors {

    /** 응답에 담는 최대 오류 수. */
    static final int MAX_ERRORS = 100;

    private final List<FieldErrorDetail> errors = new ArrayList<>();
    /** 잘린 것을 포함한 전체 오류 수. */
    private int total;

    /**
     * 오류를 추가한다.
     *
     * @param field  위치 (예: {@code "3행 영문약어"}, 행 전체 오류는 {@code "3행"})
     * @param reason 사유
     */
    public void add(String field, String reason) {
        total++;
        if (errors.size() < MAX_ERRORS) {
            errors.add(new FieldErrorDetail(field, reason));
        }
    }

    /** 담긴 오류 (최대 {@value #MAX_ERRORS}건). */
    public List<FieldErrorDetail> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** 오류가 하나라도 있으면 EXCEL_INVALID_ROWS 예외를 던진다. 호출한 트랜잭션은 이 예외로 롤백된다. */
    public void throwIfAny() {
        if (total > 0) {
            throw new ExcelException(ErrorCode.EXCEL_INVALID_ROWS, errors);
        }
    }
}
