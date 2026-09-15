package com.step.dictionary.excel;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 엑셀 시트 양식. 다운로드와 업로드가 같은 열 배치를 쓰므로, 내려받은 파일을 고쳐 그대로 올릴 수 있다.
 *
 * <p>헤더 이름 끝에 "(참고)"가 붙은 열은 다운로드에서만 채우는 계산 값이고 업로드에서는 무시한다.
 * 업로드 시 첫 행의 헤더가 이 목록과 정확히 같아야 한다 (뒤에 붙은 추가 열은 무시).
 * 열 순서를 바꾸면 각 서비스의 열 번호 상수도 함께 바꿔야 한다.
 */
public enum ExcelSheet {

    /** 표준 단어. 분류어 여부는 Y/N, 비우면 N. */
    WORD("words", "단어", List.of("단어명", "영문약어", "영문명", "분류어 여부(Y/N)", "설명")),

    /** 표준 도메인. 분류어는 단어명으로 지정한다. */
    DOMAIN("domains", "도메인", List.of("도메인명", "분류어 단어명", "데이터 타입", "길이", "소수점", "설명")),

    /** 표준 용어. 구성 단어는 단어명을 조합 순서대로 쉼표로 구분한다. 용어명/영문약어는 자동 생성 값이라 참고용이다. */
    TERM("terms", "용어", List.of("구성 단어명(쉼표 구분)", "도메인명", "설명", "용어명(참고)", "영문약어(참고)")),

    /**
     * 테이블/컬럼 정의. 한 행이 컬럼 하나이고, 같은 테이블 물리명의 행을 묶어 테이블 하나로 본다.
     * 컬럼 열이 모두 빈 행은 컬럼 없는 테이블이다. 테이블 논리명/설명은 그 테이블의 첫 행 값을 쓴다.
     */
    TABLE("tables", "테이블 정의", List.of(
            "테이블 물리명", "테이블 논리명", "테이블 설명", "컬럼 순서", "컬럼 물리명", "컬럼 논리명", "용어명",
            "데이터 타입", "길이", "소수점", "NULL 허용(Y/N)", "기본키(Y/N)", "비표준 사유(참고)"));

    /** URL 경로 이름 ({@code /api/excel/{path}/template}). */
    private final String path;
    /** 시트 이름이자 다운로드 파일 이름의 앞부분. */
    private final String sheetName;
    /** 헤더 목록 (열 순서). */
    private final List<String> headers;

    ExcelSheet(String path, String sheetName, List<String> headers) {
        this.path = path;
        this.sheetName = sheetName;
        this.headers = headers;
    }

    /** URL 경로 이름으로 양식을 찾는다. */
    public static Optional<ExcelSheet> fromPath(String path) {
        return Arrays.stream(values()).filter(sheet -> sheet.path.equals(path)).findFirst();
    }

    public String getSheetName() {
        return sheetName;
    }

    public List<String> getHeaders() {
        return headers;
    }
}
