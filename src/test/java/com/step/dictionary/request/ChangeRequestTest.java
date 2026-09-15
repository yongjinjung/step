package com.step.dictionary.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChangeRequestTest {

    private final AppUser user = new AppUser("user", "hash", "사용자", Role.USER);
    private final AppUser admin = new AppUser("admin", "hash", "관리자", Role.ADMIN);
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 15, 10, 0);

    @Test
    @DisplayName("신청하면 대기 상태이고 신청자를 확인할 수 있다")
    void create() {
        ChangeRequest request = createRequest();

        assertThat(request.getStatus()).isEqualTo(RequestStatus.REQUESTED);
        assertThat(request.isPending()).isTrue();
        assertThat(request.isRequestedBy("user")).isTrue();
        assertThat(request.isRequestedBy("admin")).isFalse();
    }

    @Test
    @DisplayName("승인하면 검토자, 의견, 처리 시각, 반영된 대상 ID가 기록된다")
    void approve() {
        ChangeRequest request = createRequest();

        request.approve(admin, "확인", now, 10L);

        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getReviewer()).isSameAs(admin);
        assertThat(request.getReviewComment()).isEqualTo("확인");
        assertThat(request.getReviewedAt()).isEqualTo(now);
        assertThat(request.getTargetId()).isEqualTo(10L);
        assertThat(request.isPending()).isFalse();
    }

    @Test
    @DisplayName("반려하면 검토자와 의견이 기록되고, 취소하면 검토자 없이 처리 시각만 기록된다")
    void rejectAndCancel() {
        ChangeRequest rejected = createRequest();
        rejected.reject(admin, "중복 단어", now);
        assertThat(rejected.getStatus()).isEqualTo(RequestStatus.REJECTED);
        assertThat(rejected.getReviewComment()).isEqualTo("중복 단어");

        ChangeRequest canceled = createRequest();
        canceled.cancel(now);
        assertThat(canceled.getStatus()).isEqualTo(RequestStatus.CANCELED);
        assertThat(canceled.getReviewer()).isNull();
        assertThat(canceled.getReviewedAt()).isEqualTo(now);
    }

    private ChangeRequest createRequest() {
        return new ChangeRequest(TargetType.WORD, RequestType.CREATE, null, null, "{}", "사유", user);
    }
}
