package com.step.dictionary.code.value;

import com.step.dictionary.code.group.CodeGroup;
import com.step.dictionary.code.group.CodeGroupRepository;
import com.step.dictionary.code.value.dto.CodeValueRequest;
import com.step.dictionary.code.value.dto.CodeValueResponse;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.history.ChangeHistoryRecorder;
import com.step.dictionary.history.snapshot.CodeValueSnapshot;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공통 코드값 업무 로직.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>코드값은 대문자로 정규화한 뒤 그룹 안에서 유일해야 한다 ({@code DUPLICATE_CODE_VALUE}). 다른 그룹에는 같은 값이 있어도 된다.</li>
 *   <li>경로의 그룹에 속하지 않은 코드값은 없는 것으로 본다 (404).</li>
 *   <li>코드값을 참조하는 다른 엔티티가 없어 삭제 제한은 없다 (컬럼은 코드값이 아니라 코드 그룹에 매핑한다).</li>
 *   <li>등록·수정·삭제마다 변경 이력을 남긴다 (같은 트랜잭션).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class CodeValueService {

    private final CodeValueRepository valueRepository;
    /** 소속 그룹 확인용. */
    private final CodeGroupRepository groupRepository;
    /** 변경 이력 기록. */
    private final ChangeHistoryRecorder historyRecorder;

    public CodeValueService(CodeValueRepository valueRepository, CodeGroupRepository groupRepository,
            ChangeHistoryRecorder historyRecorder) {
        this.valueRepository = valueRepository;
        this.groupRepository = groupRepository;
        this.historyRecorder = historyRecorder;
    }

    /**
     * 코드 그룹에 코드값을 등록한다.
     *
     * @throws BusinessException CODE_GROUP_NOT_FOUND (404), DUPLICATE_CODE_VALUE (409)
     */
    @Transactional
    public CodeValueResponse create(Long groupId, CodeValueRequest request) {
        CodeGroup group = getGroup(groupId);
        String codeValue = CodeValue.normalizeCodeValue(request.codeValue());
        if (valueRepository.existsByGroupIdAndCodeValue(groupId, codeValue)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_VALUE);
        }

        CodeValue value = valueRepository.save(new CodeValue(group, codeValue, request.valueName(),
                request.description(), request.sortOrder(), request.used()));
        historyRecorder.recordCreate(CodeValueSnapshot.from(value));
        return CodeValueResponse.from(value);
    }

    /**
     * 그룹의 전체 코드값을 정렬 순서대로 돌려준다. 미사용 값도 포함하며 페이징하지 않는다.
     *
     * @throws BusinessException CODE_GROUP_NOT_FOUND (404). 그룹은 있는데 코드값이 없으면 빈 목록
     */
    public List<CodeValueResponse> findAll(Long groupId) {
        // 그룹이 없으면 404로 끝낸다. 코드값 조회 결과에는 그룹이 조인되어 함께 채워진다.
        getGroup(groupId);
        return valueRepository.findAllByGroupIdOrderBySortOrderAscIdAsc(groupId).stream()
                .map(CodeValueResponse::from)
                .toList();
    }

    /**
     * 코드값을 단건 조회한다.
     *
     * @throws BusinessException CODE_VALUE_NOT_FOUND (404). 다른 그룹의 코드값이어도 404
     */
    public CodeValueResponse findById(Long groupId, Long valueId) {
        return CodeValueResponse.from(getValue(groupId, valueId));
    }

    /**
     * 코드값을 수정한다 (소속 그룹 외 전체 교체).
     *
     * @throws BusinessException CODE_VALUE_NOT_FOUND (404), DUPLICATE_CODE_VALUE (409)
     */
    @Transactional
    public CodeValueResponse update(Long groupId, Long valueId, CodeValueRequest request) {
        CodeValue value = getValue(groupId, valueId);
        // 변경 이력용으로 수정 전 값을 떠 둔다.
        CodeValueSnapshot before = CodeValueSnapshot.from(value);
        // 엔티티를 변경하기 전에 검사해 중복이면 저장하지 않고 409로 끝낸다.
        String codeValue = CodeValue.normalizeCodeValue(request.codeValue());
        if (valueRepository.existsByGroupIdAndCodeValueAndIdNot(groupId, codeValue, valueId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_CODE_VALUE);
        }

        value.update(codeValue, request.valueName(), request.description(), request.sortOrder(), request.used());
        // MyBatis는 변경 감지가 없으므로 직접 update한다. updatedAt도 함께 갱신되어 응답에 쓰인다.
        valueRepository.save(value);
        historyRecorder.recordUpdate(before, CodeValueSnapshot.from(value));
        return CodeValueResponse.from(value);
    }

    /**
     * 코드값을 삭제한다.
     *
     * @throws BusinessException CODE_VALUE_NOT_FOUND (404)
     */
    @Transactional
    public void delete(Long groupId, Long valueId) {
        CodeValue value = getValue(groupId, valueId);
        // 삭제 전 값으로 이력을 남긴다.
        historyRecorder.recordDelete(CodeValueSnapshot.from(value));
        valueRepository.delete(value);
    }

    /** ID로 코드 그룹을 찾고, 없으면 CODE_GROUP_NOT_FOUND(404)를 던진다. */
    private CodeGroup getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_GROUP_NOT_FOUND));
    }

    /** 경로의 그룹에 속하지 않은 코드값은 없는 것으로 본다. */
    private CodeValue getValue(Long groupId, Long valueId) {
        return valueRepository.findByIdAndGroupId(valueId, groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CODE_VALUE_NOT_FOUND));
    }
}
