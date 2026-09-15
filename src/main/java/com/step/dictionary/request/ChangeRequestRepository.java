package com.step.dictionary.request;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 변경 신청 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 동적 검색은 {@link ChangeRequestRepositoryCustom}이 맡는다.
 */
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long>, ChangeRequestRepositoryCustom {

    /**
     * 신청자와 검토자를 함께 조회한다. 응답에 두 사용자의 아이디/이름이 들어가므로 추가 조회를 막는다.
     * 메서드 이름의 {@code WithUsers}는 설명용이며 Spring Data가 무시한다({@code find...By} 사이의 단어).
     */
    @EntityGraph(attributePaths = {"requester", "reviewer"})
    Optional<ChangeRequest> findWithUsersById(Long id);

    /**
     * 같은 대상에 특정 상태의 신청이 있는지 확인한다.
     * {@code (대상 종류, 대상 ID, REQUESTED)}로 호출해 대기 중인 신청 중복을 막는다 (인덱스 idx_std_request_target).
     */
    boolean existsByTargetTypeAndTargetIdAndStatus(TargetType targetType, Long targetId, RequestStatus status);
}
