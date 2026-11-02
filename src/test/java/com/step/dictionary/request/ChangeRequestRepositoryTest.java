package com.step.dictionary.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.JpaConfig;
import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.support.TestRoles;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ChangeRequestRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 10, 0);

    @Autowired
    private ChangeRequestRepository requestRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private ChangeRequest wordCreate;
    private ChangeRequest wordUpdate;
    private ChangeRequest domainDelete;

    /**
     * wordCreate:   user, WORD CREATE, 대기
     * wordUpdate:   user, WORD UPDATE(5), 반려
     * domainDelete: admin, DOMAIN DELETE(7), 대기
     */
    @BeforeEach
    void setUp() {
        AppUser admin = userRepository.save(new AppUser("admin", "hash", "관리자", java.util.List.of()));
        AppUser user = userRepository.save(new AppUser("user", "hash", "사용자", java.util.List.of()));

        wordCreate = requestRepository.save(
                new ChangeRequest(TargetType.WORD, RequestType.CREATE, null, null, "{}", null, user));
        wordUpdate = new ChangeRequest(TargetType.WORD, RequestType.UPDATE, 5L, NOW, "{}", null, user);
        wordUpdate.reject(admin, "중복", NOW);
        requestRepository.save(wordUpdate);
        domainDelete = requestRepository.save(
                new ChangeRequest(TargetType.DOMAIN, RequestType.DELETE, 7L, NOW, null, null, admin));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("조건 없이 검색하면 최신순으로 모든 신청이 나온다")
    void searchAll() {
        assertThat(search(new ChangeRequestSearchCondition(null, null, null, null)))
                .containsExactly(domainDelete.getId(), wordUpdate.getId(), wordCreate.getId());
    }

    @Test
    @DisplayName("상태, 대상 종류, 신청 종류, 신청자(대소문자 무시)로 필터링한다")
    void searchByCondition() {
        assertThat(search(new ChangeRequestSearchCondition(RequestStatus.REQUESTED, null, null, null)))
                .containsExactly(domainDelete.getId(), wordCreate.getId());
        assertThat(search(new ChangeRequestSearchCondition(null, TargetType.DOMAIN, null, null)))
                .containsExactly(domainDelete.getId());
        assertThat(search(new ChangeRequestSearchCondition(null, null, RequestType.UPDATE, null)))
                .containsExactly(wordUpdate.getId());
        assertThat(search(new ChangeRequestSearchCondition(null, null, null, "USER")))
                .containsExactly(wordUpdate.getId(), wordCreate.getId());
    }

    @Test
    @DisplayName("같은 대상에 대기 중인 신청이 있는지 확인한다 (반려된 신청은 제외)")
    void existsPending() {
        assertThat(requestRepository.existsByTargetTypeAndTargetIdAndStatus(TargetType.DOMAIN, 7L, RequestStatus.REQUESTED))
                .isTrue();
        assertThat(requestRepository.existsByTargetTypeAndTargetIdAndStatus(TargetType.WORD, 5L, RequestStatus.REQUESTED))
                .isFalse();
    }

    @Test
    @DisplayName("단건 조회 시 신청자와 검토자를 함께 가져온다")
    void findWithUsers() {
        ChangeRequest found = requestRepository.findWithUsersById(wordUpdate.getId()).orElseThrow();

        assertThat(found.getRequester().getUsername()).isEqualTo("user");
        assertThat(found.getReviewer().getUsername()).isEqualTo("admin");
        assertThat(found.getTargetUpdatedAt()).isEqualTo(NOW);
    }

    private List<Long> search(ChangeRequestSearchCondition condition) {
        return requestRepository.search(condition, PageRequest.of(0, 20)).map(ChangeRequest::getId).getContent();
    }
}
