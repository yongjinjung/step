package com.step.dictionary.code.group;

import com.step.dictionary.code.group.dto.CodeGroupColumnResponse;
import com.step.dictionary.code.group.dto.CodeGroupRequest;
import com.step.dictionary.code.group.dto.CodeGroupResponse;
import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import com.step.dictionary.code.value.CodeValue;
import com.step.dictionary.code.value.CodeValueRepository;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.CodeGroupSnapshot;
import com.step.dictionary.history.snapshot.CodeValueSnapshot;
import com.step.dictionary.meta.column.MetaColumnRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공통 코드 그룹 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>코드ID는 대문자로 정규화한 뒤 전체에서 유일해야 한다 ({@code DUPLICATE_CODE_ID}).</li>
 *   <li>컬럼 정의에 매핑된 코드 그룹은 삭제할 수 없다 ({@code CODE_GROUP_IN_USE}).
 *       매핑은 그룹 ID(FK)로 연결되므로 코드ID·코드명·사용 여부는 매핑 중에도 바꿀 수 있다.</li>
 *   <li>코드 그룹을 삭제하면 속한 코드값도 함께 삭제하고, 코드값마다 삭제 이력을 남긴다.</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (같은 트랜잭션).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CodeGroupService {

    private final CodeGroupRepository groupRepository;
    /** 그룹 삭제 시 코드값 조회·삭제용. */
    private final CodeValueRepository valueRepository;
    /** 매핑된 컬럼 조회와 삭제 차단용. */
    private final MetaColumnRepository columnRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public CodeGroupService(CodeGroupRepository groupRepository, CodeValueRepository valueRepository,
            MetaColumnRepository columnRepository, ChangeHistoryRecorder historyRecorder) {
        this.groupRepository = groupRepository;
        this.valueRepository = valueRepository;
        this.columnRepository = columnRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 코드 그룹을 등록한다.
     *
     * @throws BusinessException DUPLICATE_CODE_ID (409)
     */
    @Transactional
    public CodeGroupResponse create(CodeGroupRequest request) {
        // 정규화한 값으로 검사해야 order_stat과 ORDER_STAT을 같은 코드ID로 판단한다.
        String codeId = CodeGroup.normalizeCodeId(request.codeId());
        if (groupRepository.existsByCodeId(codeId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_ID);
        }

        CodeGroup group = groupRepository.save(
                new CodeGroup(codeId, request.codeName(), request.description(), request.used()));
        historyRecorder.recordCreate(CodeGroupSnapshot.from(group));
        return CodeGroupResponse.from(group);
    }

    /**
     * 코드 그룹을 단건 조회한다.
     *
     * @throws BusinessException CODE_GROUP_NOT_FOUND (404)
     */
    public CodeGroupResponse findById(Long id) {
        return CodeGroupResponse.from(getGroup(id));
    }

    /** 조건으로 코드 그룹을 검색한다. 코드ID 오름차순. */
    public PageResponse<CodeGroupResponse> search(CodeGroupSearchCondition condition, Pageable pageable) {
        return PageResponse.from(groupRepository.search(condition, pageable).map(CodeGroupResponse::from));
    }

    /**
     * 코드 그룹에 매핑된 컬럼 정의 목록. 테이블 물리명, 컬럼 순서로 정렬하며 페이징하지 않는다.
     * 코드 그룹을 삭제하기 전에 어디서 쓰는지 확인하는 데 쓴다.
     *
     * @throws BusinessException CODE_GROUP_NOT_FOUND (404). 그룹은 있는데 매핑이 없으면 빈 목록
     */
    public List<CodeGroupColumnResponse> findColumns(Long id) {
        // 없는 그룹과 매핑이 없는 그룹을 구분하기 위해 먼저 존재를 확인한다.
        getGroup(id);
        return columnRepository.findAllByCodeGroupIdOrderByTablePhysicalNameAscSeqAscIdAsc(id).stream()
                .map(CodeGroupColumnResponse::from)
                .toList();
    }

    /**
     * 코드 그룹을 수정한다 (전체 교체).
     *
     * @throws BusinessException CODE_GROUP_NOT_FOUND (404), DUPLICATE_CODE_ID (409)
     */
    @Transactional
    public CodeGroupResponse update(Long id, CodeGroupRequest request) {
        CodeGroup group = getGroup(id);
        // 변경 이력용으로 수정 전 값을 떠 둔다.
        CodeGroupSnapshot before = CodeGroupSnapshot.from(group);
        // 엔티티를 변경하기 전에 검사해야 한다. 변경 후 조회하면 auto flush로 unique 제약 위반이 먼저 터진다.
        String codeId = CodeGroup.normalizeCodeId(request.codeId());
        if (groupRepository.existsByCodeIdAndIdNot(codeId, id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_ID);
        }

        group.update(codeId, request.codeName(), request.description(), request.used());
        // 즉시 update를 실행해 갱신된 updatedAt으로 응답한다.
        groupRepository.flush();
        historyRecorder.recordUpdate(before, CodeGroupSnapshot.from(group));
        return CodeGroupResponse.from(group);
    }

    /**
     * 코드 그룹과 속한 코드값을 삭제한다.
     *
     * <p>순서는 {@code MetaTableService.delete}와 같다: 코드값을 불러와 값마다 삭제 이력을 남기고 엔티티 단위로 삭제한 뒤,
     * 그룹 이력을 남기고 그룹을 지운다. 코드값 삭제를 먼저 등록해야 flush 때 FK 위반이 없다.
     * JPQL 벌크 delete는 불러온 코드값이 영속성 컨텍스트에 남아 {@code TransientPropertyValueException}이 나므로 쓰지 않는다.
     *
     * @throws BusinessException CODE_GROUP_NOT_FOUND (404), CODE_GROUP_IN_USE (409)
     */
    @Transactional
    public void delete(Long id) {
        CodeGroup group = getGroup(id);
        // DB FK 위반(500)에 맡기지 않고 매핑 여부를 먼저 확인해 409로 응답한다.
        if (columnRepository.existsByCodeGroupId(id)) {
            throw new BusinessException(ErrorCode.CODE_GROUP_IN_USE);
        }
        List<CodeValue> values = valueRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(id);
        values.forEach(value -> historyRecorder.recordDelete(CodeValueSnapshot.from(value)));
        valueRepository.deleteAll(values);
        historyRecorder.recordDelete(CodeGroupSnapshot.from(group));
        groupRepository.delete(group);
    }

    /** ID로 코드 그룹을 찾고, 없으면 CODE_GROUP_NOT_FOUND(404)를 던진다. */
    private CodeGroup getGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_GROUP_NOT_FOUND));
    }
}
