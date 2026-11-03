package com.step.dictionary.meta.table;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.ColumnSnapshot;
import com.step.dictionary.history.snapshot.TableSnapshot;
import com.step.dictionary.meta.PhysicalNames;
import com.step.dictionary.meta.column.MetaColumn;
import com.step.dictionary.meta.column.MetaColumnRepository;
import com.step.dictionary.meta.table.dto.MetaTableRequest;
import com.step.dictionary.meta.table.dto.MetaTableResponse;
import com.step.dictionary.meta.table.dto.MetaTableSearchCondition;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테이블 정의 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>물리명은 대문자로 정규화한 뒤 전체에서 유일해야 한다 ({@code DUPLICATE_TABLE_NAME}).</li>
 *   <li>테이블을 삭제하면 그 컬럼 정의도 함께 삭제한다. 삭제되는 컬럼마다 삭제 이력을 남긴다.</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (7단계, 같은 트랜잭션).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MetaTableService {

    private final MetaTableRepository tableRepository;
    /** 테이블 삭제 시 컬럼 조회·삭제용. */
    private final MetaColumnRepository columnRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public MetaTableService(MetaTableRepository tableRepository, MetaColumnRepository columnRepository,
            ChangeHistoryRecorder historyRecorder) {
        this.tableRepository = tableRepository;
        this.columnRepository = columnRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 테이블 정의를 등록한다.
     *
     * @throws BusinessException DUPLICATE_TABLE_NAME (409)
     */
    @Transactional
    public MetaTableResponse create(MetaTableRequest request) {
        // 대문자로 바꾼 값으로 검사해야 tb_customer와 TB_CUSTOMER를 같은 이름으로 판단한다.
        String physicalName = PhysicalNames.normalize(request.physicalName());
        if (tableRepository.existsByPhysicalName(physicalName)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TABLE_NAME);
        }

        MetaTable table = tableRepository.save(
                new MetaTable(physicalName, request.logicalName(), request.description()));
        historyRecorder.recordCreate(TableSnapshot.from(table));
        return MetaTableResponse.from(table);
    }

    /**
     * 테이블 정의를 단건 조회한다.
     *
     * @throws BusinessException TABLE_NOT_FOUND (404)
     */
    public MetaTableResponse findById(Long id) {
        return MetaTableResponse.from(getTable(id));
    }

    /** 조건으로 테이블 정의를 검색한다. 물리명 오름차순. */
    public PageResponse<MetaTableResponse> search(MetaTableSearchCondition condition, Pageable pageable) {
        return PageResponse.from(tableRepository.search(condition, pageable).map(MetaTableResponse::from));
    }

    /**
     * 테이블 정의를 수정한다 (전체 교체).
     *
     * @throws BusinessException TABLE_NOT_FOUND (404), DUPLICATE_TABLE_NAME (409)
     */
    @Transactional
    public MetaTableResponse update(Long id, MetaTableRequest request) {
        MetaTable table = getTable(id);
        // 변경 이력용으로 수정 전 값을 떠 둔다.
        TableSnapshot before = TableSnapshot.from(table);
        // 엔티티를 변경하기 전에 검사해 중복이면 저장하지 않고 409로 끝낸다.
        String physicalName = PhysicalNames.normalize(request.physicalName());
        if (tableRepository.existsByPhysicalNameAndIdNot(physicalName, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_TABLE_NAME);
        }

        table.update(physicalName, request.logicalName(), request.description());
        // MyBatis는 변경 감지가 없으므로 직접 update한다. updatedAt도 함께 갱신되어 응답에 쓰인다.
        tableRepository.save(table);
        historyRecorder.recordUpdate(before, TableSnapshot.from(table));
        return MetaTableResponse.from(table);
    }

    /**
     * 컬럼 정의는 테이블에 속하므로 함께 삭제한다.
     *
     * <p>순서:
     * <ol>
     *   <li>컬럼을 불러와 컬럼마다 삭제 이력을 남긴다.</li>
     *   <li>컬럼을 지운다. 테이블보다 먼저 지워야 FK 위반이 없다.</li>
     *   <li>테이블 삭제 이력을 남기고 테이블을 지운다.</li>
     * </ol>
     *
     * @throws BusinessException TABLE_NOT_FOUND (404)
     */
    @Transactional
    public void delete(Long id) {
        MetaTable table = getTable(id);
        List<MetaColumn> columns = columnRepository.findAllByTableIdOrderBySeqAscIdAsc(id);
        columns.forEach(column -> historyRecorder.recordDelete(ColumnSnapshot.from(column)));
        columnRepository.deleteAll(columns);
        historyRecorder.recordDelete(TableSnapshot.from(table));
        tableRepository.delete(table);
    }

    /** ID로 테이블 정의를 찾고, 없으면 TABLE_NOT_FOUND(404)를 던진다. */
    private MetaTable getTable(Long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TABLE_NOT_FOUND));
    }
}
