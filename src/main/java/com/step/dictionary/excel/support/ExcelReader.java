package com.step.dictionary.excel.support;

import com.step.dictionary.excel.ExcelSheet;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.error.ErrorResponse.FieldErrorDetail;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드된 .xlsx 파일을 읽어 데이터 행 목록으로 만든다.
 *
 * <p>규칙:
 * <ul>
 *   <li>첫 번째 시트만 읽는다. 1행은 헤더이고 {@link ExcelSheet}의 헤더와 같아야 한다.</li>
 *   <li>모든 셀을 엑셀 화면에 보이는 문자열로 읽는다({@link DataFormatter}). 숫자 20은 "20", 수식은 계산 결과다.</li>
 *   <li>값이 하나도 없는 행은 건너뛴다. 행 번호는 엑셀에서 보이는 번호를 그대로 유지한다.</li>
 *   <li>데이터 행은 1행 이상 {@value #MAX_UPLOAD_ROWS}행 이하여야 한다.</li>
 * </ul>
 * 파일 전체를 메모리에 올리는 XSSF 방식이므로 업로드 크기는 multipart 설정(10MB)으로 제한한다.
 */
public final class ExcelReader {

    /** 업로드 최대 데이터 행 수. 한 트랜잭션에서 처리하므로 너무 크면 잠금과 응답 시간이 길어진다. */
    public static final int MAX_UPLOAD_ROWS = 1_000;

    /** 인스턴스 생성 방지. */
    private ExcelReader() {
    }

    /**
     * 파일을 읽는다.
     *
     * @param file   업로드 파일
     * @param sheet  기대하는 양식
     * @param errors 행 오류를 모을 곳. 반환된 행들이 이 객체에 오류를 기록한다
     * @return 값이 있는 데이터 행 목록
     * @throws ExcelException EXCEL_INVALID_FILE, EXCEL_INVALID_HEADER, EXCEL_EMPTY, EXCEL_TOO_MANY_ROWS (400)
     */
    public static List<ExcelRow> read(MultipartFile file, ExcelSheet sheet, ExcelErrors errors) {
        try (InputStream in = file.getInputStream(); Workbook workbook = new XSSFWorkbook(in)) {
            return readRows(workbook, sheet, errors);
        } catch (ExcelException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // 빈 파일, .xls, 텍스트 파일, 손상된 zip 등 POI가 열지 못하는 모든 경우.
            throw new ExcelException(ErrorCode.EXCEL_INVALID_FILE);
        }
    }

    private static List<ExcelRow> readRows(Workbook workbook, ExcelSheet sheet, ExcelErrors errors) {
        Sheet worksheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        List<String> headers = sheet.getHeaders();

        List<String> actualHeaders = cellTexts(worksheet.getRow(0), headers.size(), formatter, evaluator);
        if (!actualHeaders.equals(headers)) {
            throw new ExcelException(ErrorCode.EXCEL_INVALID_HEADER,
                    List.of(new FieldErrorDetail("헤더", "필요한 헤더: " + String.join(", ", headers))));
        }

        List<ExcelRow> rows = new ArrayList<>();
        for (int index = 1; index <= worksheet.getLastRowNum(); index++) {
            List<String> values = cellTexts(worksheet.getRow(index), headers.size(), formatter, evaluator);
            if (values.stream().allMatch(String::isEmpty)) {
                continue;
            }
            if (rows.size() == MAX_UPLOAD_ROWS) {
                throw new ExcelException(ErrorCode.EXCEL_TOO_MANY_ROWS);
            }
            // POI 행 인덱스는 0부터, 엑셀 화면의 행 번호는 1부터다.
            rows.add(new ExcelRow(index + 1, headers, values, errors));
        }
        if (rows.isEmpty()) {
            throw new ExcelException(ErrorCode.EXCEL_EMPTY);
        }
        return rows;
    }

    /** 행의 앞 {@code count}개 셀을 화면에 보이는 문자열로 읽는다. 행이나 셀이 없으면 빈 문자열. */
    private static List<String> cellTexts(Row row, int count, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<String> texts = new ArrayList<>(count);
        for (int column = 0; column < count; column++) {
            Cell cell = row == null ? null : row.getCell(column);
            texts.add(cell == null ? "" : formatter.formatCellValue(cell, evaluator).trim());
        }
        return texts;
    }
}
