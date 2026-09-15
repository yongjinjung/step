package com.step.dictionary.history;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.dto.ChangeHistoryResponse;
import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 변경 이력 조회 (7단계). 이력 저장은 {@link ChangeHistoryRecorder}가 한다.
 * 로그인한 사용자는 누구나 조회할 수 있다 (표준 데이터와 테이블 정의를 모두가 조회하므로 이력도 공개한다).
 */
@Service
@Transactional(readOnly = true)
public class ChangeHistoryService {

    private final ChangeHistoryRepository historyRepository;
    /** 스냅샷 JSON을 응답용 트리로 바꾸는 데 쓴다. */
    private final JsonMapper jsonMapper;

    public ChangeHistoryService(ChangeHistoryRepository historyRepository, JsonMapper jsonMapper) {
        this.historyRepository = historyRepository;
        this.jsonMapper = jsonMapper;
    }

    /**
     * 이력을 단건 조회한다.
     *
     * @throws BusinessException HISTORY_NOT_FOUND (404)
     */
    public ChangeHistoryResponse findById(Long id) {
        return historyRepository.findWithUserById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.HISTORY_NOT_FOUND));
    }

    /** 조건으로 이력을 검색한다. 최신순. */
    public PageResponse<ChangeHistoryResponse> search(ChangeHistorySearchCondition condition, Pageable pageable) {
        return PageResponse.from(historyRepository.search(condition, pageable).map(this::toResponse));
    }

    /** 응답으로 변환한다. 스냅샷을 JSON 객체로 바꾸고 달라진 필드를 계산한다. */
    private ChangeHistoryResponse toResponse(ChangeHistory history) {
        JsonNode before = readTree(history.getBeforeData());
        JsonNode after = readTree(history.getAfterData());
        return ChangeHistoryResponse.from(history, before, after, ChangedFields.between(before, after));
    }

    private JsonNode readTree(String json) {
        return json == null ? null : jsonMapper.readTree(json);
    }
}
