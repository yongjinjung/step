package com.step.dictionary.code.value;

import com.step.dictionary.code.value.dto.CodeValueRequest;
import com.step.dictionary.code.value.dto.CodeValueResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
 * 공통 코드값 REST API ({@code /api/code-groups/{groupId}/values}, 코드 그룹의 하위 리소스).
 * 권한: 조회는 로그인한 사용자, 등록·수정·삭제는 ADMIN ({@code SecurityConfig}의 {@code /api/code-groups/**} 규칙).
 */
@Tag(name = "공통 코드값", description = "코드 그룹에 속한 코드값 관리")
@RestController
@RequestMapping("/api/code-groups/{groupId}/values")
public class CodeValueController {

    private final CodeValueService valueService;

    public CodeValueController(CodeValueService valueService) {
        this.valueService = valueService;
    }

    /** 코드값 등록. 201 Created + Location 헤더({@code /api/code-groups/{groupId}/values/{id}}). */
    @Operation(summary = "코드값 등록", description = "코드값은 대문자로 저장한다.")
    @PostMapping
    public ResponseEntity<CodeValueResponse> create(
            @PathVariable Long groupId, @Valid @RequestBody CodeValueRequest request) {
        CodeValueResponse response = valueService.create(groupId, request);
        return ResponseEntity.created(URI.create("/api/code-groups/" + groupId + "/values/" + response.id()))
                .body(response);
    }

    /** 그룹의 전체 코드값 목록 (페이징 없음). 그룹이 없으면 404. */
    @Operation(summary = "코드값 목록", description = "정렬 순서(sortOrder) 오름차순. 미사용 코드값도 포함한다.")
    @GetMapping
    public List<CodeValueResponse> findAll(@PathVariable Long groupId) {
        return valueService.findAll(groupId);
    }

    /** 코드값 단건 조회. 다른 그룹의 코드값이면 404. */
    @Operation(summary = "코드값 단건 조회")
    @GetMapping("/{valueId}")
    public CodeValueResponse findById(@PathVariable Long groupId, @PathVariable Long valueId) {
        return valueService.findById(groupId, valueId);
    }

    /** 코드값 수정 (전체 교체). 소속 그룹은 바꿀 수 없다. */
    @Operation(summary = "코드값 수정")
    @PutMapping("/{valueId}")
    public CodeValueResponse update(
            @PathVariable Long groupId, @PathVariable Long valueId, @Valid @RequestBody CodeValueRequest request) {
        return valueService.update(groupId, valueId, request);
    }

    /** 코드값 삭제. 성공 시 204. */
    @Operation(summary = "코드값 삭제")
    @DeleteMapping("/{valueId}")
    public ResponseEntity<Void> delete(@PathVariable Long groupId, @PathVariable Long valueId) {
        valueService.delete(groupId, valueId);
        return ResponseEntity.noContent().build();
    }
}
