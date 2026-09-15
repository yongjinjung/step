package com.step.dictionary.excel.support;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 업로드 파일의 데이터 행 하나. 셀 문자열을 요청 DTO에 넣을 값으로 바꾸고, 실패하면 오류를 기록한다.
 *
 * <p>변환 메서드({@link #integer}, {@link #yesNo}, {@link #enumValue})는 실패해도 예외를 던지지 않고
 * "{행}행 {헤더}" 위치로 오류를 남긴 뒤 null(또는 false)을 돌려준다. 한 행의 여러 문제를 한 번에 알려주기 위해서다.
 * 변환에 실패한 열은 기억해 두었다가 {@link #validate}에서 같은 열의 검증 오류를 중복으로 남기지 않는다.
 */
public final class ExcelRow {

    /** 엑셀에서 보이는 행 번호 (헤더가 1행이므로 데이터는 2행부터). */
    private final int rowNumber;
    private final List<String> headers;
    /** 헤더 수만큼의 셀 문자열. 빈 셀은 빈 문자열. */
    private final List<String> values;
    private final ExcelErrors errors;
    /** 변환·조회에 실패해 이미 오류를 남긴 열 번호. */
    private final Set<Integer> failedColumns = new HashSet<>();
    private boolean hasErrors;

    public ExcelRow(int rowNumber, List<String> headers, List<String> values, ExcelErrors errors) {
        this.rowNumber = rowNumber;
        this.headers = headers;
        this.values = values;
        this.errors = errors;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    /** 셀 문자열. 앞뒤 공백을 제거하며, 비어 있으면 null. */
    public String text(int column) {
        String value = column < values.size() ? values.get(column) : null;
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 정수. 비어 있으면 null, 정수가 아니면 오류를 남기고 null. */
    public Integer integer(int column) {
        String value = text(column);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            error(column, "정수여야 합니다: " + value);
            return null;
        }
    }

    /**
     * Y/N 값. 비어 있으면 N(false)으로 본다. Y/N 외에 엑셀 논리값(TRUE/FALSE)도 받는다. 대소문자는 구분하지 않는다.
     * 그 밖의 값이면 오류를 남기고 false.
     */
    public boolean yesNo(int column) {
        String value = text(column);
        if (value == null) {
            return false;
        }
        return switch (value.toUpperCase(Locale.ROOT)) {
            case "Y", "TRUE" -> true;
            case "N", "FALSE" -> false;
            default -> {
                error(column, "Y 또는 N이어야 합니다: " + value);
                yield false;
            }
        };
    }

    /** enum 값 (대소문자 구분 없음). 비어 있으면 null, 없는 값이면 허용 값 목록과 함께 오류를 남기고 null. */
    public <E extends Enum<E>> E enumValue(int column, Class<E> type) {
        String value = text(column);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            String allowed = Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
            error(column, "허용 값은 " + allowed + " 입니다: " + value);
            return null;
        }
    }

    /** 특정 열의 오류. 위치는 "{행}행 {헤더}"다. */
    public void error(int column, String message) {
        failedColumns.add(column);
        add(rowNumber + "행 " + headers.get(column), message);
    }

    /** 행 전체의 오류 (예: 서비스의 업무 규칙 위반). 위치는 "{행}행"이다. */
    public void error(String message) {
        add(rowNumber + "행", message);
    }

    /** 이 행에 오류가 하나라도 있는지. 오류가 있는 행은 반영하지 않는다. */
    public boolean hasErrors() {
        return hasErrors;
    }

    /**
     * 요청 DTO를 Bean Validation으로 검사하고, 위반을 해당 열의 오류로 남긴다.
     * 위반은 열 순서대로 기록해 응답 순서가 일정하게 한다. 이미 변환에 실패한 열의 위반은 건너뛴다.
     *
     * @param validator    스프링이 등록한 검증기
     * @param target       검사할 요청 DTO (API 요청 본문과 같은 규칙)
     * @param fieldColumns DTO 필드 이름 → 열 번호. 없는 필드의 위반은 행 전체 오류로 남긴다
     * @return 이 행에 오류가 전혀 없으면 true (이전 변환 오류도 포함해 판단)
     */
    public <T> boolean validate(Validator validator, T target, Map<String, Integer> fieldColumns) {
        validator.validate(target).stream()
                .sorted(Comparator.comparing(
                        (ConstraintViolation<T> violation) -> fieldColumns.getOrDefault(fieldName(violation), Integer.MAX_VALUE)))
                .forEach(violation -> {
                    Integer column = fieldColumns.get(fieldName(violation));
                    if (column == null) {
                        error(violation.getMessage());
                    } else if (!failedColumns.contains(column)) {
                        error(column, violation.getMessage());
                    }
                });
        return !hasErrors;
    }

    /** 위반 경로의 첫 이름 (예: {@code wordIds[0]} → {@code wordIds}). */
    private static String fieldName(ConstraintViolation<?> violation) {
        return violation.getPropertyPath().iterator().next().getName();
    }

    private void add(String field, String message) {
        hasErrors = true;
        errors.add(field, message);
    }
}
