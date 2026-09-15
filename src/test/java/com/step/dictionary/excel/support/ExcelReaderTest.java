package com.step.dictionary.excel.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.step.dictionary.excel.ExcelSheet;
import com.step.dictionary.global.error.ErrorCode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ExcelReaderTest {

    @Test
    @DisplayName("쓴 파일을 읽으면 헤더를 확인하고, 빈 행은 건너뛰며 엑셀 행 번호를 유지한다")
    void writeAndRead() {
        byte[] bytes = ExcelWriter.write(ExcelSheet.WORD, List.of(
                Arrays.asList("고객", "CUST", "Customer", "N", null),
                Arrays.asList(null, null, null, null, null),
                Arrays.asList("번호", "NO", "Number", "Y", 20)));

        List<ExcelRow> rows = ExcelReader.read(file(bytes), ExcelSheet.WORD, new ExcelErrors());

        assertThat(rows).extracting(ExcelRow::getRowNumber).containsExactly(2, 4);
        assertThat(rows.get(0).text(0)).isEqualTo("고객");
        assertThat(rows.get(0).text(4)).isNull();
        assertThat(rows.get(1).text(4)).isEqualTo("20");
    }

    @Test
    @DisplayName("헤더가 양식과 다르면 필요한 헤더를 알려준다")
    void invalidHeader() {
        byte[] bytes = ExcelWriter.write("단어", List.of("이름", "약어"), List.of(List.of("고객", "CUST")));

        assertThatThrownBy(() -> ExcelReader.read(file(bytes), ExcelSheet.WORD, new ExcelErrors()))
                .isInstanceOf(ExcelException.class)
                .satisfies(e -> {
                    ExcelException excel = (ExcelException) e;
                    assertThat(excel.getErrorCode()).isEqualTo(ErrorCode.EXCEL_INVALID_HEADER);
                    assertThat(excel.getDetails().getFirst().reason()).contains("단어명, 영문약어");
                });
    }

    @Test
    @DisplayName(".xlsx가 아닌 파일은 읽을 수 없다")
    void invalidFile() {
        assertThatThrownBy(() -> ExcelReader.read(file("단어명,영문약어".getBytes()), ExcelSheet.WORD, new ExcelErrors()))
                .isInstanceOf(ExcelException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXCEL_INVALID_FILE);
    }

    @Test
    @DisplayName("데이터 행이 없거나 1,000행을 넘으면 거부한다")
    void rowCount() {
        byte[] empty = ExcelWriter.write(ExcelSheet.WORD, List.of());
        assertThatThrownBy(() -> ExcelReader.read(file(empty), ExcelSheet.WORD, new ExcelErrors()))
                .isInstanceOf(ExcelException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXCEL_EMPTY);

        List<List<String>> tooMany = IntStream.rangeClosed(1, ExcelReader.MAX_UPLOAD_ROWS + 1)
                .mapToObj(i -> List.of("단어" + i, "W" + i, "Word", "N", "설명"))
                .toList();
        byte[] large = ExcelWriter.write(ExcelSheet.WORD, tooMany);
        assertThatThrownBy(() -> ExcelReader.read(file(large), ExcelSheet.WORD, new ExcelErrors()))
                .isInstanceOf(ExcelException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXCEL_TOO_MANY_ROWS);
    }

    private static MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile("file", "upload.xlsx", null, bytes);
    }
}
