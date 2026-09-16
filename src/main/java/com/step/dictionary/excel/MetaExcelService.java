package com.step.dictionary.excel;

import static java.util.Map.entry;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import com.step.dictionary.excel.dto.ExcelUploadResponse;
import com.step.dictionary.excel.support.ExcelErrors;
import com.step.dictionary.excel.support.ExcelReader;
import com.step.dictionary.excel.support.ExcelRow;
import com.step.dictionary.excel.support.ExcelWriter;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.meta.PhysicalNames;
import com.step.dictionary.meta.column.MetaColumn;
import com.step.dictionary.meta.column.MetaColumnRepository;
import com.step.dictionary.meta.column.MetaColumnService;
import com.step.dictionary.meta.column.dto.MetaColumnRequest;
import com.step.dictionary.meta.table.MetaTable;
import com.step.dictionary.meta.table.MetaTableRepository;
import com.step.dictionary.meta.table.MetaTableService;
import com.step.dictionary.meta.table.dto.MetaTableRequest;
import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.term.TermRepository;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 테이블/컬럼 정의 엑셀 다운로드와 업로드 (8단계).
 *
 * <p>한 행이 컬럼 하나이고, 같은 테이블 물리명(대소문자 무시)의 행을 묶어 테이블 하나로 등록한다.
 * 테이블/컬럼 정의는 USER도 직접 쓸 수 있으므로 업로드는 역할과 관계없이 바로 반영한다.
 *
 * <p>업로드 규칙:
 * <ul>
 *   <li>등록만 한다. 이미 있는 테이블 물리명이면 중복 오류가 난다.</li>
 *   <li>테이블 논리명/설명은 그 테이블의 첫 행 값을 쓴다.</li>
 *   <li>컬럼 열(컬럼 순서~기본키)이 모두 빈 행은 컬럼 없는 테이블을 뜻한다.</li>
 *   <li>컬럼 순서를 비우면 그 테이블 안에서 컬럼 행이 나온 순서(1부터)를 쓴다. NULL 허용/기본키를 비우면 N이다.</li>
 *   <li>용어는 용어명으로 지정하며 이미 등록돼 있어야 한다. 비우면 매핑하지 않는다.</li>
 *   <li>전부 반영하거나 전혀 반영하지 않는다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MetaExcelService {

    // ---- 열 번호 (ExcelSheet.TABLE 헤더 순서와 같아야 한다) ----
    private static final int TABLE_PHYSICAL_NAME = 0;
    private static final int TABLE_LOGICAL_NAME = 1;
    private static final int TABLE_DESCRIPTION = 2;
    private static final int COLUMN_SEQ = 3;
    private static final int COLUMN_PHYSICAL_NAME = 4;
    private static final int COLUMN_LOGICAL_NAME = 5;
    private static final int TERM_NAME = 6;
    private static final int DATA_TYPE = 7;
    private static final int DATA_LENGTH = 8;
    private static final int DATA_SCALE = 9;
    private static final int NULLABLE = 10;
    private static final int PRIMARY_KEY = 11;

    private static final Map<String, Integer> TABLE_FIELDS = Map.of(
            "physicalName", TABLE_PHYSICAL_NAME, "logicalName", TABLE_LOGICAL_NAME, "description", TABLE_DESCRIPTION);
    private static final Map<String, Integer> COLUMN_FIELDS = Map.ofEntries(
            entry("seq", COLUMN_SEQ), entry("physicalName", COLUMN_PHYSICAL_NAME),
            entry("logicalName", COLUMN_LOGICAL_NAME), entry("termId", TERM_NAME), entry("dataType", DATA_TYPE),
            entry("dataLength", DATA_LENGTH), entry("dataScale", DATA_SCALE), entry("nullable", NULLABLE),
            entry("primaryKey", PRIMARY_KEY));

    private final MetaTableRepository tableRepository;
    private final MetaColumnRepository columnRepository;
    private final TermRepository termRepository;
    private final MetaTableService tableService;
    private final MetaColumnService columnService;
    private final Validator validator;

    public MetaExcelService(MetaTableRepository tableRepository, MetaColumnRepository columnRepository,
            TermRepository termRepository, MetaTableService tableService, MetaColumnService columnService,
            Validator validator) {
        this.tableRepository = tableRepository;
        this.columnRepository = columnRepository;
        this.termRepository = termRepository;
        this.tableService = tableService;
        this.columnService = columnService;
        this.validator = validator;
    }

    /**
     * 테이블 검색 결과와 그 컬럼을 엑셀로 만든다. 테이블은 물리명 순, 컬럼은 순서(seq) 순이다.
     * 컬럼 없는 테이블은 컬럼 열이 빈 행 하나로 쓴다. 비표준 사유(참고) 열에는 현재 기준 검출 결과가 들어간다.
     */
    public byte[] downloadTables(MetaTableSearchCondition condition) {
        Page<MetaTable> page = tableRepository.search(condition, ExcelWriter.downloadPage());
        ExcelWriter.requireWithinLimit(page);
        List<MetaTable> tables = page.getContent();

        // 모든 테이블의 컬럼을 한 번에 조회해 테이블별로 묶는다. IN 절에 빈 목록을 넘기지 않는다.
        Map<Long, List<MetaColumn>> columnsByTable = tables.isEmpty()
                ? Map.of()
                : columnRepository.findAllByTableIdInOrderBySeqAscIdAsc(tables.stream().map(MetaTable::getId).toList())
                        .stream()
                        .collect(groupingBy(column -> column.getTable().getId()));

        List<List<Object>> rows = new ArrayList<>();
        for (MetaTable table : tables) {
            List<MetaColumn> columns = columnsByTable.getOrDefault(table.getId(), List.of());
            if (columns.isEmpty()) {
                rows.add(Arrays.asList(table.getPhysicalName(), table.getLogicalName(), table.getDescription(),
                        null, null, null, null, null, null, null, null, null, null));
            }
            for (MetaColumn column : columns) {
                rows.add(columnRow(table, column));
            }
        }
        return ExcelWriter.write(ExcelSheet.TABLE, rows);
    }

    /**
     * 테이블/컬럼 정의를 업로드한다.
     *
     * @throws com.step.dictionary.excel.support.ExcelException 파일/헤더/행 수 오류, 또는 오류 행이 있을 때 (400, 전체 롤백)
     */
    @Transactional
    public ExcelUploadResponse uploadTables(MultipartFile file) {
        ExcelErrors errors = new ExcelErrors();
        List<ExcelRow> rows = ExcelReader.read(file, ExcelSheet.TABLE, errors);

        // 테이블 물리명별로 행을 묶는다. 파일에 나온 순서를 유지한다.
        Map<String, List<ExcelRow>> rowsByTable = new LinkedHashMap<>();
        for (ExcelRow row : rows) {
            String physicalName = row.text(TABLE_PHYSICAL_NAME);
            if (physicalName == null) {
                row.error(TABLE_PHYSICAL_NAME, "필수 값입니다.");
                continue;
            }
            rowsByTable.computeIfAbsent(PhysicalNames.normalize(physicalName), key -> new ArrayList<>()).add(row);
        }

        List<Long> tableIds = new ArrayList<>();
        for (List<ExcelRow> tableRows : rowsByTable.values()) {
            Long tableId = createTable(tableRows.getFirst());
            if (tableId == null) {
                // 테이블을 만들지 못하면 그 테이블의 컬럼은 처리하지 않는다 (오류는 첫 행에 남았다).
                continue;
            }
            tableIds.add(tableId);
            createColumns(tableId, tableRows);
        }

        errors.throwIfAny();
        return ExcelUploadResponse.applied(rows.size(), tableIds);
    }

    /** 그룹의 첫 행 값으로 테이블을 만든다. 실패하면 오류를 남기고 null. */
    private Long createTable(ExcelRow firstRow) {
        MetaTableRequest request = new MetaTableRequest(firstRow.text(TABLE_PHYSICAL_NAME),
                firstRow.text(TABLE_LOGICAL_NAME), firstRow.text(TABLE_DESCRIPTION));
        if (!firstRow.validate(validator, request, TABLE_FIELDS)) {
            return null;
        }
        try {
            return tableService.create(request).id();
        } catch (BusinessException e) {
            firstRow.error(e.getErrorCode().getMessage());
            return null;
        }
    }

    /** 테이블의 컬럼 행들을 순서대로 등록한다. 컬럼 열이 모두 빈 행은 건너뛴다. */
    private void createColumns(Long tableId, List<ExcelRow> tableRows) {
        int position = 0;
        for (ExcelRow row : tableRows) {
            if (!hasColumn(row)) {
                continue;
            }
            position++;
            Integer seq = row.text(COLUMN_SEQ) == null ? Integer.valueOf(position) : row.integer(COLUMN_SEQ);
            // 엑셀 양식에는 공통 코드 열이 없어 코드 그룹은 매핑하지 않는다(null). 매핑은 화면·API에서 한다.
            MetaColumnRequest request = new MetaColumnRequest(row.text(COLUMN_PHYSICAL_NAME),
                    row.text(COLUMN_LOGICAL_NAME), findTermId(row), null, row.enumValue(DATA_TYPE, DataType.class),
                    row.integer(DATA_LENGTH), row.integer(DATA_SCALE), row.yesNo(NULLABLE), row.yesNo(PRIMARY_KEY), seq);
            if (!row.validate(validator, request, COLUMN_FIELDS)) {
                continue;
            }
            try {
                columnService.create(tableId, request);
            } catch (BusinessException e) {
                row.error(e.getErrorCode().getMessage());
            }
        }
    }

    /** 컬럼 열(컬럼 순서~기본키) 중 값이 하나라도 있는지. */
    private static boolean hasColumn(ExcelRow row) {
        for (int column = COLUMN_SEQ; column <= PRIMARY_KEY; column++) {
            if (row.text(column) != null) {
                return true;
            }
        }
        return false;
    }

    /** 용어명 열로 용어 ID를 찾는다. 비어 있으면 매핑 없음(null), 없는 용어면 오류를 남기고 null. */
    private Long findTermId(ExcelRow row) {
        String name = row.text(TERM_NAME);
        if (name == null) {
            return null;
        }
        return termRepository.findByName(name).map(Term::getId).orElseGet(() -> {
            row.error(TERM_NAME, "용어 '" + name + "'을(를) 찾을 수 없습니다.");
            return null;
        });
    }

    /** 컬럼 한 행. 비표준 사유는 사유 이름을 쉼표로 잇고, 표준이면 빈 칸이다. */
    private static List<Object> columnRow(MetaTable table, MetaColumn column) {
        String issues = column.inspect().stream().map(Enum::name).collect(joining(", "));
        return Arrays.asList(table.getPhysicalName(), table.getLogicalName(), table.getDescription(),
                column.getSeq(), column.getPhysicalName(), column.getLogicalName(),
                column.getTerm() == null ? null : column.getTerm().getName(),
                column.getDataType().name(), column.getDataLength(), column.getDataScale(),
                column.isNullable() ? "Y" : "N", column.isPrimaryKey() ? "Y" : "N",
                issues.isEmpty() ? null : issues);
    }
}
