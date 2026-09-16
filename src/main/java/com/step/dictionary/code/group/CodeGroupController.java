package com.step.dictionary.code.group;

import com.step.dictionary.code.group.dto.CodeGroupColumnResponse;
import com.step.dictionary.code.group.dto.CodeGroupRequest;
import com.step.dictionary.code.group.dto.CodeGroupResponse;
import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import com.step.dictionary.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공통 코드 그룹 REST API ({@code /api/code-groups}).
 * 권한: 조회는 로그인한 사용자, 등록·수정·삭제는 ADMIN ({@code SecurityConfig}).
 */
@Tag(name = "공통 코드 그룹", description = "공통 코드 그룹(코드ID) 관리와 매핑된 컬럼 조회")
@RestController
@RequestMapping("/api/code-groups")
public class CodeGroupController {

    private final CodeGroupService groupService;

    public CodeGroupController(CodeGroupService groupService) {
        this.groupService = groupService;
    }

    /** 코드 그룹 등록. 201 Created + Location 헤더. */
    @Operation(summary = "코드 그룹 등록", description = "코드ID는 대문자로 저장한다.")
    @PostMapping
    public ResponseEntity<CodeGroupResponse> create(@Valid @RequestBody CodeGroupRequest request) {
        CodeGroupResponse response = groupService.create(request);
        return ResponseEntity.created(URI.create("/api/code-groups/" + response.id())).body(response);
    }

    /** 코드 그룹 단건 조회. 없으면 404. */
    @Operation(summary = "코드 그룹 단건 조회")
    @GetMapping("/{id}")
    public CodeGroupResponse findById(@PathVariable Long id) {
        return groupService.findById(id);
    }

    /** 코드 그룹 검색 ({@code ?keyword=&used=&page=&size=}). 기본 20건. */
    @Operation(summary = "코드 그룹 검색", description = "keyword는 코드ID/코드명에 부분 일치. 코드ID 오름차순 정렬.")
    @GetMapping
    public PageResponse<CodeGroupResponse> search(
            @ParameterObject CodeGroupSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return groupService.search(condition, pageable);
    }

    /** 코드 그룹을 쓰는 컬럼 정의 목록 (페이징 없음). */
    @Operation(summary = "코드 그룹 매핑 컬럼 목록", description = "이 코드 그룹에 매핑된 컬럼 정의. 테이블 물리명, 컬럼 순서로 정렬.")
    @GetMapping("/{id}/columns")
    public List<CodeGroupColumnResponse> findColumns(@PathVariable Long id) {
        return groupService.findColumns(id);
    }

    /** 코드 그룹 수정 (전체 교체). */
    @Operation(summary = "코드 그룹 수정")
    @PutMapping("/{id}")
    public CodeGroupResponse update(@PathVariable Long id, @Valid @RequestBody CodeGroupRequest request) {
        return groupService.update(id, request);
    }

    /** 코드 그룹 삭제. 코드값도 함께 삭제한다. 성공 시 204, 컬럼에 매핑돼 있으면 409. */
    @Operation(summary = "코드 그룹 삭제", description = "속한 코드값도 함께 삭제한다. 컬럼에 매핑된 그룹은 삭제할 수 없다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
