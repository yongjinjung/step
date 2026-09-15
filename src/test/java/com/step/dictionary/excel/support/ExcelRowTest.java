package com.step.dictionary.excel.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.excel.ExcelSheet;
import com.step.dictionary.global.error.ErrorResponse.FieldErrorDetail;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.word.dto.WordRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExcelRowTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final ExcelErrors errors = new ExcelErrors();

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("정수와 enum 변환에 실패하면 '행 번호 + 헤더' 위치로 오류를 남기고 null을 돌려준다")
    void conversionErrors() {
        ExcelRow row = row(ExcelSheet.DOMAIN, "번호V20", "번호", "VARCHR", "2O", "", "");

        assertThat(row.enumValue(2, DataType.class)).isNull();
        assertThat(row.integer(3)).isNull();
        assertThat(row.integer(4)).isNull();
        assertThat(row.enumValue(0, DataType.class)).isNull();

        assertThat(fields()).containsExactly("3행 데이터 타입", "3행 길이", "3행 도메인명");
        assertThat(errors.getErrors().getFirst().reason()).contains("VARCHAR");
    }

    @Test
    @DisplayName("Y/N은 대소문자와 TRUE/FALSE를 받고, 비어 있으면 N으로 본다")
    void yesNo() {
        assertThat(row(ExcelSheet.WORD, "", "", "", "y", "").yesNo(3)).isTrue();
        assertThat(row(ExcelSheet.WORD, "", "", "", "TRUE", "").yesNo(3)).isTrue();
        assertThat(row(ExcelSheet.WORD, "", "", "", "n", "").yesNo(3)).isFalse();
        assertThat(row(ExcelSheet.WORD, "", "", "", "", "").yesNo(3)).isFalse();
        assertThat(errors.getErrors()).isEmpty();

        ExcelRow invalid = row(ExcelSheet.WORD, "", "", "", "예", "");
        assertThat(invalid.yesNo(3)).isFalse();
        assertThat(invalid.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("검증 위반은 필드에 맞는 헤더로 열 순서대로 남긴다")
    void validate() {
        ExcelRow row = row(ExcelSheet.WORD, "", "1ab", "Customer", "N", "");
        WordRequest request = new WordRequest(null, "1ab", "Customer", null, false);

        boolean valid = row.validate(validator, request, Map.of("name", 0, "abbreviation", 1, "englishName", 2));

        assertThat(valid).isFalse();
        assertThat(fields()).containsExactly("3행 단어명", "3행 영문약어");
    }

    @Test
    @DisplayName("이미 변환·조회에 실패한 열의 검증 위반은 다시 남기지 않는다")
    void skipFailedColumn() {
        ExcelRow row = row(ExcelSheet.DOMAIN, "번호V20", "없는단어", "VARCHAR", "20", "", "");
        row.error(1, "단어 '없는단어'을(를) 찾을 수 없습니다.");
        DomainRequest request = new DomainRequest("번호V20", null, DataType.VARCHAR, 20, null, null);

        boolean valid = row.validate(validator, request, Map.of("name", 0, "classifierWordId", 1));

        assertThat(valid).isFalse();
        assertThat(fields()).containsExactly("3행 분류어 단어명");
    }

    private ExcelRow row(ExcelSheet sheet, String... values) {
        return new ExcelRow(3, sheet.getHeaders(), Arrays.asList(values), errors);
    }

    private List<String> fields() {
        return errors.getErrors().stream().map(FieldErrorDetail::field).toList();
    }
}
