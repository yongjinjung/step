package com.step.dictionary.excel.support;

import com.step.dictionary.excel.ExcelSheet;
import com.step.dictionary.global.error.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * .xlsx 파일을 만든다. 다운로드와 업로드 양식 파일에 쓴다.
 *
 * <p>SXSSF(스트리밍) 방식이라 메모리에 최근 {@value #ROW_ACCESS_WINDOW}행만 두고 나머지는 임시 파일로 내린다.
 * 다운로드 건수는 {@value #MAX_DOWNLOAD_ROWS}건으로 제한한다.
 */
public final class ExcelWriter {

    /** 다운로드 최대 건수. 넘으면 검색 조건을 좁히도록 400을 준다. */
    public static final int MAX_DOWNLOAD_ROWS = 10_000;

    /** 메모리에 유지하는 행 수 (SXSSF). */
    private static final int ROW_ACCESS_WINDOW = 100;

    /** 인스턴스 생성 방지. */
    private ExcelWriter() {
    }

    /** 양식의 헤더로 파일을 만든다. 행이 비어 있으면 헤더만 있는 업로드 양식이 된다. */
    public static byte[] write(ExcelSheet sheet, List<? extends List<?>> rows) {
        return write(sheet.getSheetName(), sheet.getHeaders(), rows);
    }

    /**
     * 파일을 만든다.
     *
     * @param sheetName 시트 이름
     * @param headers   1행 헤더
     * @param rows      데이터 행. 각 값은 null(빈 셀), 숫자(숫자 셀), 그 밖에는 문자열 셀로 쓴다
     */
    public static byte[] write(String sheetName, List<String> headers, List<? extends List<?>> rows) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW);
        // close()가 SXSSF 임시 파일까지 정리한다.
        try (workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            writeHeader(workbook, sheet, headers);

            int rowIndex = 1;
            for (List<?> values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < values.size(); column++) {
                    writeCell(row, column, values.get(column));
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            // 메모리 스트림에 쓰므로 실제로는 임시 파일 문제일 때만 난다.
            throw new UncheckedIOException(e);
        }
    }

    /** 다운로드 조회용 페이지 요청. 최대 건수만큼 한 번에 가져온다. */
    public static PageRequest downloadPage() {
        return PageRequest.of(0, MAX_DOWNLOAD_ROWS);
    }

    /** 조건에 맞는 전체 건수가 다운로드 최대 건수를 넘으면 EXCEL_TOO_MANY_ROWS(400). 일부만 잘린 파일을 주지 않는다. */
    public static void requireWithinLimit(Page<?> page) {
        if (page.getTotalElements() > MAX_DOWNLOAD_ROWS) {
            throw new ExcelException(ErrorCode.EXCEL_TOO_MANY_ROWS);
        }
    }

    /** 굵은 글씨 + 회색 배경 헤더를 쓰고, 헤더 길이에 맞춰 열 너비를 정하고, 헤더 행을 틀 고정한다. */
    private static void writeHeader(SXSSFWorkbook workbook, Sheet sheet, List<String> headers) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Row row = sheet.createRow(0);
        for (int column = 0; column < headers.size(); column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(headers.get(column));
            cell.setCellStyle(style);
            // 너비 단위는 글자 1/256. 한글은 영문보다 넓어 헤더 글자 수의 2배를 잡는다.
            sheet.setColumnWidth(column, Math.min(60, Math.max(12, headers.get(column).length() * 2 + 2)) * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void writeCell(Row row, int column, Object value) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(column);
        if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
