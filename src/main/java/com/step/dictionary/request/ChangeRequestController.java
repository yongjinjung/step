package com.step.dictionary.request;

import com.step.dictionary.auth.CurrentUser;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.request.dto.ApproveForm;
import com.step.dictionary.request.dto.ChangeRequestForm;
import com.step.dictionary.request.dto.ChangeRequestResponse;
import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import com.step.dictionary.request.dto.RejectForm;
import com.step.dictionary.standard.domain.dto.DomainRequest;
import com.step.dictionary.standard.term.dto.TermRequest;
import com.step.dictionary.standard.word.dto.WordRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 표준 데이터 변경 신청/승인 REST API ({@code /api/requests}).
 *
 * <p>신청은 대상 종류별 경로로 받는다({@code /words}, {@code /domains}, {@code /terms}).
 * 경로마다 신청 내용의 타입이 달라 {@code @Valid}로 기존 요청 DTO 규칙을 그대로 검증할 수 있고, Swagger에도 타입별 스키마가 나온다.
 * 승인/반려는 ADMIN 전용이며 {@code SecurityConfig}의 URL 규칙이 막는다.
 */
@Tag(name = "신청/승인", description = "표준 단어/도메인/용어 변경 신청과 관리자 승인")
@RestController
@RequestMapping("/api/requests")
public class ChangeRequestController {

    private final ChangeRequestService requestService;

    public ChangeRequestController(ChangeRequestService requestService) {
        this.requestService = requestService;
    }

    /** 단어 변경 신청. 201 Created + Location. */
    @Operation(summary = "단어 변경 신청", description = "payload는 단어 등록/수정 API의 요청 본문과 같다. 삭제 신청은 payload를 비운다.")
    @PostMapping("/words")
    public ResponseEntity<ChangeRequestResponse> requestWord(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeRequestForm<WordRequest> form) {
        return created(requestService.create(TargetType.WORD, form, jwt.getSubject()));
    }

    /** 도메인 변경 신청. 201 Created + Location. */
    @Operation(summary = "도메인 변경 신청", description = "payload는 도메인 등록/수정 API의 요청 본문과 같다. 삭제 신청은 payload를 비운다.")
    @PostMapping("/domains")
    public ResponseEntity<ChangeRequestResponse> requestDomain(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeRequestForm<DomainRequest> form) {
        return created(requestService.create(TargetType.DOMAIN, form, jwt.getSubject()));
    }

    /** 용어 변경 신청. 201 Created + Location. */
    @Operation(summary = "용어 변경 신청", description = "payload는 용어 등록/수정 API의 요청 본문과 같다. 삭제 신청은 payload를 비운다.")
    @PostMapping("/terms")
    public ResponseEntity<ChangeRequestResponse> requestTerm(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangeRequestForm<TermRequest> form) {
        return created(requestService.create(TargetType.TERM, form, jwt.getSubject()));
    }

    /** 신청 검색 ({@code ?status=&targetType=&requestType=&requester=&page=&size=}). 최신순, 기본 20건. */
    @Operation(summary = "신청 검색", description = "최신순. USER는 자기 신청만, ADMIN은 전체를 조회한다(requester로 신청자 아이디 필터).")
    @GetMapping
    public PageResponse<ChangeRequestResponse> search(@AuthenticationPrincipal Jwt jwt,
            @ParameterObject ChangeRequestSearchCondition condition,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return requestService.search(condition, pageable, CurrentUser.from(jwt));
    }

    /** 신청 단건 조회. USER가 남의 신청을 조회하면 404. */
    @Operation(summary = "신청 단건 조회")
    @GetMapping("/{id}")
    public ChangeRequestResponse findById(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return requestService.findById(id, CurrentUser.from(jwt));
    }

    /** 승인 (ADMIN). 본문은 선택이며 {@code {"comment"}}를 담을 수 있다. */
    @Operation(summary = "신청 승인 (관리자)",
            description = "대상에 반영한다. 신청 이후 대상이 바뀌었으면 409 REQUEST_TARGET_CHANGED, "
                    + "반영 중 업무 규칙 위반이면 해당 오류를 반환하고 신청은 대기 상태로 남는다.")
    @PostMapping("/{id}/approve")
    public ChangeRequestResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody(required = false) ApproveForm form) {
        return requestService.approve(id, form, jwt.getSubject());
    }

    /** 반려 (ADMIN). 의견 필수. */
    @Operation(summary = "신청 반려 (관리자)", description = "반려 의견(comment)이 필요하다.")
    @PostMapping("/{id}/reject")
    public ChangeRequestResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody RejectForm form) {
        return requestService.reject(id, form, jwt.getSubject());
    }

    /** 본인 신청 취소. */
    @Operation(summary = "신청 취소", description = "신청자 본인만, 대기 중인 신청만 취소할 수 있다.")
    @PostMapping("/{id}/cancel")
    public ChangeRequestResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return requestService.cancel(id, CurrentUser.from(jwt));
    }

    /** 201 Created와 Location 헤더를 만든다. */
    private static ResponseEntity<ChangeRequestResponse> created(ChangeRequestResponse response) {
        return ResponseEntity.created(URI.create("/api/requests/" + response.id())).body(response);
    }
}
