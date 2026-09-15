package com.step.dictionary.meta.column;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.ColumnSnapshot;
import com.step.dictionary.meta.PhysicalNames;
import com.step.dictionary.meta.column.dto.MetaColumnRequest;
import com.step.dictionary.meta.column.dto.MetaColumnResponse;
import com.step.dictionary.meta.column.dto.NonStandardColumnCondition;
import com.step.dictionary.meta.table.MetaTable;
import com.step.dictionary.meta.table.MetaTableRepository;
import com.step.dictionary.standard.term.Term;
import com.step.dictionary.standard.term.TermRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 컬럼 정의 업무 로직과 비표준 컬럼 검색.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>물리명은 대문자로 정규화한 뒤 테이블 안에서 유일해야 한다 ({@code DUPLICATE_COLUMN_NAME}).</li>
 *   <li>타입/길이/소수점은 도메인과 같은 {@code DataType} 규칙으로 검증한다 ({@code INVALID_COLUMN_SPEC}).</li>
 *   <li>기본키 컬럼은 NULL을 허용할 수 없다 ({@code PRIMARY_KEY_NULLABLE}).</li>
 *   <li>용어 매핑은 선택이다. 컬럼 응답에는 항상 비표준 검출 결과가 포함된다.</li>
 *   <li>경로의 테이블에 속하지 않은 컬럼은 없는 것으로 본다 (404).</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (7단계, 같은 트랜잭션).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MetaColumnService {

    private final MetaColumnRepository columnRepository;
    /** 소속 테이블 확인용. */
    private final MetaTableRepository tableRepository;
    /** 매핑 용어 조회용. */
    private final TermRepository termRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public MetaColumnService(MetaColumnRepository columnRepository, MetaTableRepository tableRepository,
            TermRepository termRepository, ChangeHistoryRecorder historyRecorder) {
        this.columnRepository = columnRepository;
        this.tableRepository = tableRepository;
        this.termRepository = termRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 테이블에 컬럼 정의를 등록한다.
     *
     * @throws BusinessException INVALID_COLUMN_SPEC, PRIMARY_KEY_NULLABLE (400), TABLE_NOT_FOUND, TERM_NOT_FOUND (404),
     *                           DUPLICATE_COLUMN_NAME (409)
     */
    @Transactional
    public MetaColumnResponse create(Long tableId, MetaColumnRequest request) {
        // DB 조회 없이 할 수 있는 입력 검증을 먼저 한다.
        validate(request);
        MetaTable table = getTable(tableId);
        String physicalName = PhysicalNames.normalize(request.physicalName());
        if (columnRepository.existsByTableIdAndPhysicalName(tableId, physicalName)) {
            throw new BusinessException(ErrorCode.DUPLICATE_COLUMN_NAME);
        }
        Term term = getTerm(request.termId());

        MetaColumn column = columnRepository.save(new MetaColumn(
                table, physicalName, request.logicalName(), term, request.dataType(),
                request.dataLength(), request.dataScale(), request.nullable(), request.primaryKey(), request.seq()));
        historyRecorder.recordCreate(ColumnSnapshot.from(column));
        return MetaColumnResponse.from(column);
    }

    /**
     * 테이블의 전체 컬럼을 순서대로, 표준 검출 결과와 함께 돌려준다. 페이징하지 않는다.
     *
     * @throws BusinessException TABLE_NOT_FOUND (404). 테이블은 있는데 컬럼이 없으면 빈 목록
     */
    public List<MetaColumnResponse> findAll(Long tableId) {
        // 없는 테이블과 컬럼이 없는 테이블을 구분하기 위해 먼저 존재를 확인한다.
        getTable(tableId);
        return columnRepository.findAllByTableIdOrderBySeqAscIdAsc(tableId).stream()
                .map(MetaColumnResponse::from)
                .toList();
    }

    /**
     * 컬럼 정의를 단건 조회한다. 테이블·용어·도메인은 트랜잭션 안에서 지연 로딩된다.
     *
     * @throws BusinessException COLUMN_NOT_FOUND (404). 다른 테이블의 컬럼이어도 404
     */
    public MetaColumnResponse findById(Long tableId, Long columnId) {
        return MetaColumnResponse.from(getColumn(tableId, columnId));
    }

    /** 비표준 컬럼을 검색한다. 테이블 물리명, 컬럼 순서 오름차순. */
    public PageResponse<MetaColumnResponse> searchNonStandard(NonStandardColumnCondition condition, Pageable pageable) {
        return PageResponse.from(
                columnRepository.searchNonStandard(condition, pageable).map(MetaColumnResponse::from));
    }

    /**
     * 컬럼 정의를 수정한다 (소속 테이블 외 전체 교체).
     *
     * @throws BusinessException INVALID_COLUMN_SPEC, PRIMARY_KEY_NULLABLE (400), COLUMN_NOT_FOUND, TERM_NOT_FOUND (404),
     *                           DUPLICATE_COLUMN_NAME (409)
     */
    @Transactional
    public MetaColumnResponse update(Long tableId, Long columnId, MetaColumnRequest request) {
        validate(request);
        MetaColumn column = getColumn(tableId, columnId);
        // 변경 이력용으로 수정 전 값을 떠 둔다.
        ColumnSnapshot before = ColumnSnapshot.from(column);
        // 엔티티를 변경하기 전에 검사해야 한다. 변경 후 조회하면 auto flush로 unique 제약 위반이 먼저 터진다.
        String physicalName = PhysicalNames.normalize(request.physicalName());
        if (columnRepository.existsByTableIdAndPhysicalNameAndIdNot(tableId, physicalName, columnId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_COLUMN_NAME);
        }
        Term term = getTerm(request.termId());

        column.update(physicalName, request.logicalName(), term, request.dataType(),
                request.dataLength(), request.dataScale(), request.nullable(), request.primaryKey(), request.seq());
        // 즉시 update를 실행해 갱신된 updatedAt으로 응답한다.
        columnRepository.flush();
        historyRecorder.recordUpdate(before, ColumnSnapshot.from(column));
        return MetaColumnResponse.from(column);
    }

    /**
     * 컬럼 정의를 삭제한다. 컬럼을 참조하는 다른 엔티티가 없어 참조 검사는 하지 않는다.
     *
     * @throws BusinessException COLUMN_NOT_FOUND (404)
     */
    @Transactional
    public void delete(Long tableId, Long columnId) {
        MetaColumn column = getColumn(tableId, columnId);
        // 삭제 전 값으로 이력을 남긴다.
        historyRecorder.recordDelete(ColumnSnapshot.from(column));
        columnRepository.delete(column);
    }

    /** 타입별 길이/소수점 규칙과 기본키 NULL 규칙을 검사한다. */
    private void validate(MetaColumnRequest request) {
        if (!request.dataType().isValidSpec(request.dataLength(), request.dataScale())) {
            throw new BusinessException(ErrorCode.INVALID_COLUMN_SPEC);
        }
        if (request.primaryKey() && request.nullable()) {
            throw new BusinessException(ErrorCode.PRIMARY_KEY_NULLABLE);
        }
    }

    /** ID로 테이블 정의를 찾고, 없으면 TABLE_NOT_FOUND(404)를 던진다. */
    private MetaTable getTable(Long tableId) {
        return tableRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
    }

    /** 경로의 테이블에 속하지 않은 컬럼은 없는 것으로 본다. */
    private MetaColumn getColumn(Long tableId, Long columnId) {
        return columnRepository.findByIdAndTableId(columnId, tableId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COLUMN_NOT_FOUND));
    }

    /** 용어 ID가 없으면 매핑 없음(null), 있는데 찾을 수 없으면 TERM_NOT_FOUND(404)를 던진다. */
    private Term getTerm(Long termId) {
        if (termId == null) {
            return null;
        }
        return termRepository.findById(termId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TERM_NOT_FOUND));
    }
}
