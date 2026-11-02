package com.step.dictionary.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.step.dictionary.history.snapshot.TermSnapshot;
import com.step.dictionary.history.snapshot.WordSnapshot;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.support.TestRoles;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ChangeHistoryRecorderTest {

    private static final WordSnapshot CUSTOMER = new WordSnapshot(1L, "고객", "CUST", "Customer", null, false);

    @Mock
    private ChangeHistoryRepository historyRepository;

    @Mock
    private AppUserRepository userRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));
    private ChangeHistoryRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ChangeHistoryRecorder(historyRepository, userRepository, JsonMapper.builder().build(), clock);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("등록 이력에 스냅샷 JSON, 로그인 사용자, 변경 일시를 남긴다 (대상 종류는 JSON에 넣지 않는다)")
    void recordCreate() {
        AppUser admin = new AppUser("admin", "hash", "관리자", TestRoles.adminOnly());
        loginAs("admin");
        given(userRepository.findByUsername("admin")).willReturn(Optional.of(admin));

        recorder.recordCreate(CUSTOMER);

        ChangeHistory saved = captureSaved();
        assertThat(saved.getTargetType()).isEqualTo(HistoryTargetType.WORD);
        assertThat(saved.getTargetId()).isEqualTo(1L);
        assertThat(saved.getChangeType()).isEqualTo(ChangeType.CREATE);
        assertThat(saved.getBeforeData()).isNull();
        assertThat(saved.getAfterData())
                .isEqualTo("{\"id\":1,\"name\":\"고객\",\"abbreviation\":\"CUST\",\"englishName\":\"Customer\","
                        + "\"description\":null,\"classifier\":false}");
        assertThat(saved.getChangedBy()).isSameAs(admin);
        assertThat(saved.getRequestId()).isNull();
        assertThat(saved.getChangedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    @DisplayName("값이 하나도 바뀌지 않은 수정은 이력을 남기지 않는다")
    void skipUnchangedUpdate() {
        recorder.recordUpdate(CUSTOMER, new WordSnapshot(1L, "고객", "CUST", "Customer", null, false));

        verify(historyRepository, never()).save(any());
    }

    @Test
    @DisplayName("수정 이력에는 전후 스냅샷을 모두 남긴다")
    void recordUpdate() {
        TermSnapshot before = new TermSnapshot(3L, "고객번호", "CUST_NO", List.of(1L, 2L), 5L, "번호V20", null);
        TermSnapshot after = new TermSnapshot(3L, "고객번호", "CUST_NO", List.of(1L, 2L), 5L, "번호V20", "설명 추가");

        recorder.recordUpdate(before, after);

        ChangeHistory saved = captureSaved();
        assertThat(saved.getTargetType()).isEqualTo(HistoryTargetType.TERM);
        assertThat(saved.getChangeType()).isEqualTo(ChangeType.UPDATE);
        assertThat(saved.getBeforeData()).contains("\"wordIds\":[1,2]").contains("\"description\":null");
        assertThat(saved.getAfterData()).contains("\"description\":\"설명 추가\"");
    }

    @Test
    @DisplayName("신청 승인 중에 기록하면 신청 ID가 들어가고, 로그인 정보가 없으면 변경자는 비운다")
    void recordDeleteWithinRequest() {
        HistoryRequestContext.callWithRequest(7L, () -> {
            recorder.recordDelete(CUSTOMER);
            return null;
        });

        ChangeHistory saved = captureSaved();
        assertThat(saved.getChangeType()).isEqualTo(ChangeType.DELETE);
        assertThat(saved.getAfterData()).isNull();
        assertThat(saved.getRequestId()).isEqualTo(7L);
        assertThat(saved.getChangedBy()).isNull();
        verify(userRepository, never()).findByUsername(any());
        assertThat(HistoryRequestContext.currentRequestId()).isNull();
    }

    private void loginAs(String username) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(username).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private ChangeHistory captureSaved() {
        ArgumentCaptor<ChangeHistory> captor = ArgumentCaptor.forClass(ChangeHistory.class);
        verify(historyRepository).save(captor.capture());
        return captor.getValue();
    }
}
