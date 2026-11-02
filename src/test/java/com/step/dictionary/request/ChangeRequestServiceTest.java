package com.step.dictionary.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.auth.CurrentUser;
import com.step.dictionary.role.PermissionService;
import com.step.dictionary.role.UserPermissions;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.request.dto.ApproveForm;
import com.step.dictionary.request.dto.ChangeRequestForm;
import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import com.step.dictionary.standard.word.dto.WordRequest;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.support.TestRoles;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ChangeRequestServiceTest {

    private static final WordRequest PAYLOAD = new WordRequest("고객", "CUST", "Customer", null, false);
    private static final LocalDateTime TARGET_UPDATED_AT = LocalDateTime.of(2026, 9, 15, 9, 0);

    @Mock
    private ChangeRequestRepository requestRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private StandardDataApplier applier;

    @Mock
    private PermissionService permissionService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final AppUser user = new AppUser("user", "hash", "사용자", TestRoles.userOnly());
    private final AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());

    private ChangeRequestService service;

    @BeforeEach
    void setUp() {
        service = new ChangeRequestService(requestRepository, userRepository, applier, permissionService, clock);
    }

    @Test
    @DisplayName("신청 종류에 맞지 않는 대상 ID나 신청 내용은 거부한다")
    void createInvalidShape() {
        assertInvalidShape(new ChangeRequestForm<>(RequestType.CREATE, 1L, PAYLOAD, null));
        assertInvalidShape(new ChangeRequestForm<>(RequestType.CREATE, null, null, null));
        assertInvalidShape(new ChangeRequestForm<>(RequestType.UPDATE, null, PAYLOAD, null));
        assertInvalidShape(new ChangeRequestForm<>(RequestType.UPDATE, 1L, null, null));
        assertInvalidShape(new ChangeRequestForm<>(RequestType.DELETE, 1L, PAYLOAD, null));
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 신청은 신청 시점 대상의 수정 일시와 신청 내용 JSON을 기록한다")
    void createUpdate() {
        given(applier.findUpdatedAt(TargetType.WORD, 1L)).willReturn(Optional.of(TARGET_UPDATED_AT));
        given(userRepository.findByUsername("user")).willReturn(Optional.of(user));
        given(applier.serialize(PAYLOAD)).willReturn("{\"name\":\"고객\"}");
        given(requestRepository.save(any(ChangeRequest.class))).willAnswer(invocation -> invocation.getArgument(0));

        service.create(TargetType.WORD, new ChangeRequestForm<>(RequestType.UPDATE, 1L, PAYLOAD, "영문명 수정"), "user");

        ArgumentCaptor<ChangeRequest> saved = ArgumentCaptor.forClass(ChangeRequest.class);
        verify(requestRepository).save(saved.capture());
        assertThat(saved.getValue().getTargetUpdatedAt()).isEqualTo(TARGET_UPDATED_AT);
        assertThat(saved.getValue().getPayload()).isEqualTo("{\"name\":\"고객\"}");
        assertThat(saved.getValue().getStatus()).isEqualTo(RequestStatus.REQUESTED);
        assertThat(saved.getValue().getRequester()).isSameAs(user);
    }

    @Test
    @DisplayName("수정/삭제 신청의 대상이 없으면 대상 종류별 404 오류를 준다")
    void createTargetNotFound() {
        given(applier.findUpdatedAt(TargetType.DOMAIN, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(
                TargetType.DOMAIN, new ChangeRequestForm<>(RequestType.DELETE, 1L, null, null), "user"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DOMAIN_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 대상에 대기 중인 신청이 있으면 새로 신청할 수 없다")
    void createAlreadyPending() {
        given(applier.findUpdatedAt(TargetType.WORD, 1L)).willReturn(Optional.of(TARGET_UPDATED_AT));
        given(requestRepository.existsByTargetTypeAndTargetIdAndStatus(TargetType.WORD, 1L, RequestStatus.REQUESTED))
                .willReturn(true);

        assertThatThrownBy(() -> service.create(
                TargetType.WORD, new ChangeRequestForm<>(RequestType.DELETE, 1L, null, null), "user"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_ALREADY_PENDING);
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("등록 신청을 승인하면 대상에 반영하고 새 대상 ID와 처리 정보를 기록한다")
    void approveCreate() {
        ChangeRequest request = new ChangeRequest(TargetType.WORD, RequestType.CREATE, null, null, "{}", null, user);
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(request));
        given(applier.apply(TargetType.WORD, RequestType.CREATE, null, "{}")).willReturn(10L);
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(admin));

        service.approve(1L, new ApproveForm("확인"), "admin");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getTargetId()).isEqualTo(10L);
        assertThat(request.getReviewer()).isSameAs(admin);
        assertThat(request.getReviewedAt()).isEqualTo(LocalDateTime.now(clock));
        verify(applier, never()).findUpdatedAt(any(), any());
    }

    @Test
    @DisplayName("신청 이후 대상이 바뀌었으면 승인하지 않고 반영도 하지 않는다")
    void approveTargetChanged() {
        ChangeRequest request = updateRequest();
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(request));
        given(applier.findUpdatedAt(TargetType.WORD, 5L)).willReturn(Optional.of(TARGET_UPDATED_AT.plusSeconds(1)));

        assertThatThrownBy(() -> service.approve(1L, null, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_TARGET_CHANGED);
        verify(applier, never()).apply(any(), any(), any(), any());
        assertThat(request.isPending()).isTrue();
    }

    @Test
    @DisplayName("신청 이후 대상이 삭제됐으면 변경된 것으로 보고 승인하지 않는다")
    void approveTargetDeleted() {
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(updateRequest()));
        given(applier.findUpdatedAt(TargetType.WORD, 5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(1L, null, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_TARGET_CHANGED);
    }

    @Test
    @DisplayName("대상이 그대로면 수정 신청을 승인하고 반영한다")
    void approveUpdate() {
        ChangeRequest request = updateRequest();
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(request));
        given(applier.findUpdatedAt(TargetType.WORD, 5L)).willReturn(Optional.of(TARGET_UPDATED_AT));
        given(applier.apply(TargetType.WORD, RequestType.UPDATE, 5L, "{}")).willReturn(5L);
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(admin));

        service.approve(1L, null, "admin");

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getReviewComment()).isNull();
    }

    @Test
    @DisplayName("이미 처리된 신청은 다시 승인할 수 없다")
    void approveNotPending() {
        ChangeRequest request = updateRequest();
        request.cancel(LocalDateTime.now(clock));
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approve(1L, null, "admin"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_NOT_PENDING);
    }

    @Test
    @DisplayName("관리자라도 남의 신청은 취소할 수 없다")
    void cancelNotOwned() {
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(updateRequest()));
        // 검토 권한이 있어 신청을 볼 수는 있지만, 취소는 신청자 본인만 할 수 있다.
        given(permissionService.of("admin")).willReturn(UserPermissions.all());

        assertThatThrownBy(() -> service.cancel(1L, new CurrentUser("admin", List.of("ADMIN"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_NOT_OWNED);
    }

    @Test
    @DisplayName("일반 사용자에게 남의 신청은 없는 것으로 보인다")
    void findOthersRequestAsUser() {
        ChangeRequest adminRequest = new ChangeRequest(TargetType.WORD, RequestType.DELETE, 5L, TARGET_UPDATED_AT,
                null, null, admin);
        given(requestRepository.findWithUsersById(1L)).willReturn(Optional.of(adminRequest));
        // 신청 검토 권한이 없는 사용자다 (검토 권한이 있으면 남의 신청도 보인다).
        given(permissionService.of("user")).willReturn(UserPermissions.none());

        assertThatThrownBy(() -> service.findById(1L, new CurrentUser("user", List.of("USER"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REQUEST_NOT_FOUND);
    }

    @Test
    @DisplayName("일반 사용자가 검색하면 신청자 조건이 본인으로 바뀐다")
    void searchAsUser() {
        Pageable pageable = PageRequest.of(0, 20);
        given(requestRepository.search(any(), eq(pageable))).willReturn(Page.empty(pageable));
        given(permissionService.of("user")).willReturn(UserPermissions.none());

        service.search(new ChangeRequestSearchCondition(null, null, null, "admin"), pageable,
                new CurrentUser("user", List.of("USER")));

        ArgumentCaptor<ChangeRequestSearchCondition> condition = ArgumentCaptor.forClass(ChangeRequestSearchCondition.class);
        verify(requestRepository).search(condition.capture(), eq(pageable));
        assertThat(condition.getValue().requester()).isEqualTo("user");
    }

    private ChangeRequest updateRequest() {
        return new ChangeRequest(TargetType.WORD, RequestType.UPDATE, 5L, TARGET_UPDATED_AT, "{}", null, user);
    }

    private void assertInvalidShape(ChangeRequestForm<WordRequest> form) {
        assertThatThrownBy(() -> service.create(TargetType.WORD, form, "user"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CHANGE_REQUEST);
    }
}
