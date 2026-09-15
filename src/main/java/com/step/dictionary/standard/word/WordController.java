package com.step.dictionary.standard.word;

import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.standard.word.dto.WordRequest;
import com.step.dictionary.standard.word.dto.WordResponse;
import com.step.dictionary.standard.word.dto.WordSearchCondition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
 * 표준 단어 REST API ({@code /api/words}).
 *
 * <p>컨트롤러는 요청 검증({@code @Valid})과 HTTP 응답 형태만 담당하고 업무 규칙은 {@link WordService}에 둔다.
 * 오류 응답은 {@code GlobalExceptionHandler}가 만든다.
 */
@Tag(name = "표준 단어", description = "표준 단어사전 관리")
@RestController
@RequestMapping("/api/words")
public class WordController {

    private final WordService wordService;

    public WordController(WordService wordService) {
        this.wordService = wordService;
    }

    /** 단어 등록. 201 Created와 함께 Location 헤더에 새 리소스 경로를 담는다. */
    @Operation(summary = "단어 등록")
    @PostMapping
    public ResponseEntity<WordResponse> create(@Valid @RequestBody WordRequest request) {
        WordResponse response = wordService.create(request);
        return ResponseEntity.created(URI.create("/api/words/" + response.id())).body(response);
    }

    /** 단어 단건 조회. 없으면 404. */
    @Operation(summary = "단어 단건 조회")
    @GetMapping("/{id}")
    public WordResponse findById(@PathVariable Long id) {
        return wordService.findById(id);
    }

    /**
     * 단어 검색.
     * {@code @ParameterObject}는 쿼리 파라미터(keyword, classifier, page, size)를 Swagger 문서에 개별 항목으로 펼쳐 보여준다.
     * 페이지 크기를 지정하지 않으면 20건씩 조회한다.
     */
    @Operation(summary = "단어 검색", description = "keyword는 단어명/영문약어/영문명에 부분 일치. 단어명 오름차순 정렬.")
    @GetMapping
    public PageResponse<WordResponse> search(
            @ParameterObject WordSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return wordService.search(condition, pageable);
    }

    /** 단어 수정 (전체 교체). */
    @Operation(summary = "단어 수정")
    @PutMapping("/{id}")
    public WordResponse update(@PathVariable Long id, @Valid @RequestBody WordRequest request) {
        return wordService.update(id, request);
    }

    /** 단어 삭제. 성공 시 204 No Content, 참조 중이면 409. */
    @Operation(summary = "단어 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        wordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
