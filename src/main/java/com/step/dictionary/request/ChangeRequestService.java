package com.step.dictionary.request;

import com.step.dictionary.auth.CurrentUser;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.history.HistoryRequestContext;
import com.step.dictionary.request.dto.ApproveForm;
import com.step.dictionary.request.dto.ChangeRequestForm;
import com.step.dictionary.request.dto.ChangeRequestResponse;
import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import com.step.dictionary.request.dto.RejectForm;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 표준 데이터 변경 신청/승인 업무 로직 (6단계).
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>로그인한 사용자는 누구나 단어/도메인/용어의 등록·수정·삭제를 신청할 수 있다 (ADMIN도 가능).</li>
 *   <li>신청 종류에 맞게 대상 ID와 신청 내용이 있어야 한다 ({@code INVALID_CHANGE_REQUEST}, {@link RequestType} 참고).</li>
 *   <li>수정/삭제 신청은 대상이 있어야 하고({@code *_NOT_FOUND}), 같은 대상에 대기 중인 신청이 없어야 한다({@code REQUEST_ALREADY_PENDING}).</li>
 *   <li>승인/반려는 ADMIN만 한다 (URL 규칙, {@code SecurityConfig}). 대기 중인 신청만 처리한다({@code REQUEST_NOT_PENDING}).</li>
 *   <li>수정/삭제 신청 이후 대상이 바뀌었거나 삭제됐으면 승인하지 않는다({@code REQUEST_TARGET_CHANGED}).</li>
 *   <li>승인은 대상 반영과 같은 트랜잭션이다. 반영 중 업무 규칙 위반이 나면 원래 오류로 응답하고 신청은 대기 상태로 남는다.</li>
 *   <li>취소는 신청자 본인만 한다({@code REQUEST_NOT_OWNED}).</li>
 *   <li>USER는 자기 신청만 조회한다. 남의 신청은 없는 것으로 본다({@code REQUEST_NOT_FOUND}). ADMIN은 모두 조회한다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ChangeRequestService {

    private final ChangeRequestRepository requestRepository;
    /** 신청자/검토자 조회용. */
    private final AppUserRepository userRepository;
    /** 대상별 조회와 반영. */
    private final StandardDataApplier applier;
    /** 처리 일시 계산용 ({@code SecurityConfig}의 Clock 빈). */
    private final Clock clock;

    public ChangeRequestService(ChangeRequestRepository requestRepository, AppUserRepository userRepository,
            StandardDataApplier applier, Clock clock) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.applier = applier;
        this.clock = clock;
    }

    /**
     * 변경을 신청한다.
     *
     * @param targetType 대상 종류 (URL 경로로 정해진다)
     * @param form       신청 종류, 대상 ID, 신청 내용, 사유
     * @param username   신청자 아이디 (토큰 subject)
     * @throws BusinessException INVALID_CHANGE_REQUEST (400), WORD/DOMAIN/TERM_NOT_FOUND (404), REQUEST_ALREADY_PENDING (409)
     */
    @Transactional
    public ChangeRequestResponse create(TargetType targetType, ChangeRequestForm<?> form, String username) {
        validateShape(form);

        LocalDateTime targetUpdatedAt = null;
        if (form.requestType().requiresTargetId()) {
            // 신청 시점의 수정 일시를 기록해 두고, 승인할 때 비교해 그사이 변경을 알아낸다.
            targetUpdatedAt = applier.findUpdatedAt(targetType, form.targetId())
                    .orElseThrow(() -> new BusinessException(targetType.getNotFoundError()));
            // 같은 대상에 대기 중인 신청이 있으면 막는다. 동시에 두 신청이 들어오면 둘 다 통과할 수 있지만,
            // 먼저 승인된 쪽이 대상을 바꾸므로 나머지는 승인 시 충돌 검사에서 걸린다.
            if (requestRepository.existsByTargetTypeAndTargetIdAndStatus(
                    targetType, form.targetId(), RequestStatus.REQUESTED)) {
                throw new BusinessException(ErrorCode.REQUEST_ALREADY_PENDING);
            }
        }

        ChangeRequest request = requestRepository.save(new ChangeRequest(
                targetType, form.requestType(), form.targetId(), targetUpdatedAt,
                applier.serialize(form.payload()), form.reason(), getUser(username)));
        return toResponse(request);
    }

    /**
     * 신청을 단건 조회한다.
     *
     * @throws BusinessException REQUEST_NOT_FOUND (404). USER가 남의 신청을 조회해도 404
     */
    public ChangeRequestResponse findById(Long id, CurrentUser currentUser) {
        return toResponse(getVisibleRequest(id, currentUser));
    }

    /**
     * 신청을 검색한다. 최신순. USER는 조건의 신청자 값과 관계없이 자기 신청만 조회된다.
     */
    public PageResponse<ChangeRequestResponse> search(ChangeRequestSearchCondition condition, Pageable pageable,
            CurrentUser currentUser) {
        ChangeRequestSearchCondition effective = currentUser.admin()
                ? condition
                : condition.withRequester(currentUser.username());
        return PageResponse.from(requestRepository.search(effective, pageable).map(this::toResponse));
    }

    /**
     * 신청을 승인하고 대상에 반영한다. 전체가 한 트랜잭션이다.
     *
     * @param reviewerUsername 승인하는 관리자 아이디
     * @throws BusinessException REQUEST_NOT_FOUND (404), REQUEST_NOT_PENDING, REQUEST_TARGET_CHANGED (409),
     *                           그 밖에 반영 시 기존 서비스의 업무 규칙 오류 (예: DUPLICATE_WORD_NAME)
     */
    @Transactional
    public ChangeRequestResponse approve(Long id, ApproveForm form, String reviewerUsername) {
        ChangeRequest request = getRequest(id);
        requirePending(request);
        if (request.getRequestType().requiresTargetId()) {
            // 대상이 삭제됐으면 현재 값이 없어(null) 기록된 값과 달라지므로 같은 오류로 처리된다.
            LocalDateTime current = applier.findUpdatedAt(request.getTargetType(), request.getTargetId()).orElse(null);
            if (!Objects.equals(current, request.getTargetUpdatedAt())) {
                throw new BusinessException(ErrorCode.REQUEST_TARGET_CHANGED);
            }
        }

        // 기존 서비스가 업무 규칙 예외를 던지면 여기서 멈추고 트랜잭션이 롤백되어 신청은 대기 상태로 남는다.
        // 반영 중 기록되는 변경 이력에 이 신청 ID가 들어가도록 컨텍스트에 걸어 둔다.
        Long appliedTargetId = HistoryRequestContext.callWithRequest(request.getId(), () -> applier.apply(
                request.getTargetType(), request.getRequestType(), request.getTargetId(), request.getPayload()));

        request.approve(getUser(reviewerUsername), form == null ? null : form.comment(),
                LocalDateTime.now(clock), appliedTargetId);
        // 즉시 update를 실행해 갱신된 updatedAt으로 응답한다.
        requestRepository.flush();
        return toResponse(request);
    }

    /**
     * 신청을 반려한다. 대상은 바뀌지 않는다.
     *
     * @throws BusinessException REQUEST_NOT_FOUND (404), REQUEST_NOT_PENDING (409)
     */
    @Transactional
    public ChangeRequestResponse reject(Long id, RejectForm form, String reviewerUsername) {
        ChangeRequest request = getRequest(id);
        requirePending(request);

        request.reject(getUser(reviewerUsername), form.comment(), LocalDateTime.now(clock));
        requestRepository.flush();
        return toResponse(request);
    }

    /**
     * 본인 신청을 취소한다.
     *
     * @throws BusinessException REQUEST_NOT_FOUND (404), REQUEST_NOT_OWNED (403), REQUEST_NOT_PENDING (409)
     */
    @Transactional
    public ChangeRequestResponse cancel(Long id, CurrentUser currentUser) {
        // USER에게는 남의 신청이 404로 보이고, ADMIN은 볼 수는 있지만 취소는 못 한다(반려를 써야 한다).
        ChangeRequest request = getVisibleRequest(id, currentUser);
        if (!request.isRequestedBy(currentUser.username())) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_OWNED);
        }
        requirePending(request);

        request.cancel(LocalDateTime.now(clock));
        requestRepository.flush();
        return toResponse(request);
    }

    /** 신청 종류에 맞게 대상 ID와 신청 내용이 채워졌는지 검사한다 ({@link RequestType} 표 참고). */
    private void validateShape(ChangeRequestForm<?> form) {
        RequestType type = form.requestType();
        boolean targetIdOk = type.requiresTargetId() == (form.targetId() != null);
        boolean payloadOk = type.requiresPayload() == (form.payload() != null);
        if (!targetIdOk || !payloadOk) {
            throw new BusinessException(ErrorCode.INVALID_CHANGE_REQUEST);
        }
    }

    /** 대기 중이 아니면 REQUEST_NOT_PENDING(409). 이미 처리된 신청을 다시 처리하지 못하게 한다. */
    private void requirePending(ChangeRequest request) {
        if (!request.isPending()) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_PENDING);
        }
    }

    /** 신청자·검토자와 함께 신청을 찾고, 없으면 REQUEST_NOT_FOUND(404). */
    private ChangeRequest getRequest(Long id) {
        return requestRepository.findWithUsersById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUEST_NOT_FOUND));
    }

    /** 현재 사용자가 볼 수 있는 신청만 돌려준다. ADMIN은 전부, USER는 자기 신청만. */
    private ChangeRequest getVisibleRequest(Long id, CurrentUser currentUser) {
        ChangeRequest request = getRequest(id);
        if (!currentUser.admin() && !request.isRequestedBy(currentUser.username())) {
            throw new BusinessException(ErrorCode.REQUEST_NOT_FOUND);
        }
        return request;
    }

    /** 아이디로 사용자를 찾고, 없으면 USER_NOT_FOUND(404). 유효한 토큰이면 보통 존재한다. */
    private AppUser getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /** 응답으로 변환한다. 신청 내용 JSON 문자열을 JSON 객체로 바꿔 담는다. */
    private ChangeRequestResponse toResponse(ChangeRequest request) {
        return ChangeRequestResponse.from(request, applier.toJsonNode(request.getPayload()));
    }
}
