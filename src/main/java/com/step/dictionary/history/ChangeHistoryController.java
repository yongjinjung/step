package com.step.dictionary.history;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.dto.ChangeHistoryResponse;
import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 변경 이력 조회 REST API ({@code /api/histories}). 조회 전용이며 로그인한 사용자 누구나 쓸 수 있다.
 * 이력은 표준 데이터/테이블 정의 API가 변경될 때 자동으로 쌓이므로 등록/수정/삭제 API는 없다.
 */
@Tag(name = "변경 이력", description = "표준 단어/도메인/용어와 테이블/컬럼 정의의 변경 이력 조회")
@RestController
@RequestMapping("/api/histories")
public class ChangeHistoryController {

    private final ChangeHistoryService historyService;

    public ChangeHistoryController(ChangeHistoryService historyService) {
        this.historyService = historyService;
    }

    /** 이력 검색 ({@code ?targetType=&targetId=&changeType=&changedBy=&requestId=&from=&to=&page=&size=}). 최신순, 기본 20건. */
    @Operation(summary = "변경 이력 검색",
            description = "최신순. 특정 대상의 이력은 targetType과 targetId를 함께 지정한다. from/to는 yyyy-MM-dd이며 양 끝 날짜를 포함한다.")
    @GetMapping
    public PageResponse<ChangeHistoryResponse> search(
            @ParameterObject ChangeHistorySearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return historyService.search(condition, pageable);
    }

    /** 이력 단건 조회. */
    @Operation(summary = "변경 이력 단건 조회")
    @GetMapping("/{id}")
    public ChangeHistoryResponse findById(@PathVariable Long id) {
        return historyService.findById(id);
    }
}
