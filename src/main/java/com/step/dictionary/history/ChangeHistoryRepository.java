package com.step.dictionary.history;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 변경 이력 리포지토리. 저장은 {@link ChangeHistoryRecorder}, 조회는 {@link ChangeHistoryService}가 쓴다.
 * 동적 검색은 {@link ChangeHistoryRepositoryCustom}이 맡는다.
 */
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, Long>, ChangeHistoryRepositoryCustom {

    /** 변경자와 함께 단건 조회한다. 응답에 변경자 아이디/이름이 들어가기 때문이다. */
    @EntityGraph(attributePaths = "changedBy")
    Optional<ChangeHistory> findWithUserById(Long id);
}
