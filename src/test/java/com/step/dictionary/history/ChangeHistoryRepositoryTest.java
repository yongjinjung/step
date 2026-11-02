package com.step.dictionary.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.step.dictionary.global.config.JpaConfig;
import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.support.TestRoles;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
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
class ChangeHistoryRepositoryTest {

    private static final LocalDateTime DAY1 = LocalDateTime.of(2026, 9, 14, 23, 59, 59);
    private static final LocalDateTime DAY2 = LocalDateTime.of(2026, 9, 15, 0, 0, 0);

    @Autowired
    private ChangeHistoryRepository historyRepository;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private ChangeHistory wordCreate;
    private ChangeHistory wordUpdate;
    private ChangeHistory tableDelete;

    /**
     * wordCreate:  WORD 1 CREATE, admin, 9/14 23:59:59
     * wordUpdate:  WORD 1 UPDATE, user,  9/15 00:00:00
     * tableDelete: TABLE 1 DELETE, 변경자 없음, 9/15 00:00:00
     */
    @BeforeEach
    void setUp() {
        AppUser admin = userRepository.save(new AppUser("admin", "hash", "관리자", java.util.List.of()));
        AppUser user = userRepository.save(new AppUser("user", "hash", "사용자", java.util.List.of()));

        wordCreate = historyRepository.save(new ChangeHistory(
                HistoryTargetType.WORD, 1L, ChangeType.CREATE, null, "{}", admin, null, DAY1));
        wordUpdate = historyRepository.save(new ChangeHistory(
                HistoryTargetType.WORD, 1L, ChangeType.UPDATE, "{}", "{}", user, null, DAY2));
        tableDelete = historyRepository.save(new ChangeHistory(
                HistoryTargetType.TABLE, 1L, ChangeType.DELETE, "{}", null, null, null, DAY2));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("조건 없이 검색하면 최신순으로 모든 이력이 나오고 변경자가 없는 이력도 포함된다")
    void searchAll() {
        assertThat(search(condition(null, null, null, null, null, null)))
                .containsExactly(tableDelete.getId(), wordUpdate.getId(), wordCreate.getId());
    }

    @Test
    @DisplayName("대상 종류와 ID, 변경 종류, 변경자(대소문자 무시)로 필터링한다")
    void searchByTargetAndUser() {
        assertThat(search(condition(HistoryTargetType.WORD, 1L, null, null, null, null)))
                .containsExactly(wordUpdate.getId(), wordCreate.getId());
        assertThat(search(condition(null, null, ChangeType.DELETE, null, null, null)))
                .containsExactly(tableDelete.getId());
        assertThat(search(condition(null, null, null, "ADMIN", null, null)))
                .containsExactly(wordCreate.getId());
    }

    @Test
    @DisplayName("기간 조건은 시작일 00:00부터 종료일 하루 전체를 포함한다")
    void searchByDate() {
        LocalDate day1 = DAY1.toLocalDate();
        LocalDate day2 = DAY2.toLocalDate();

        assertThat(search(condition(null, null, null, null, day1, day1))).containsExactly(wordCreate.getId());
        assertThat(search(condition(null, null, null, null, day2, null)))
                .containsExactly(tableDelete.getId(), wordUpdate.getId());
        assertThat(search(condition(null, null, null, null, null, day1))).containsExactly(wordCreate.getId());
    }

    @Test
    @DisplayName("단건 조회 시 변경자를 함께 가져온다")
    void findWithUser() {
        ChangeHistory found = historyRepository.findWithUserById(wordUpdate.getId()).orElseThrow();

        assertThat(found.getChangedBy().getUsername()).isEqualTo("user");
        assertThat(found.getChangedAt()).isEqualTo(DAY2);
    }

    private static ChangeHistorySearchCondition condition(HistoryTargetType targetType, Long targetId,
            ChangeType changeType, String changedBy, LocalDate from, LocalDate to) {
        return new ChangeHistorySearchCondition(targetType, targetId, changeType, changedBy, null, from, to);
    }

    private List<Long> search(ChangeHistorySearchCondition condition) {
        return historyRepository.search(condition, PageRequest.of(0, 20)).map(ChangeHistory::getId).getContent();
    }
}
