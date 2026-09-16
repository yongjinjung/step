package com.step.dictionary.code.value;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 공통 코드값 리포지토리. 코드값은 그룹 안에서만 다루므로 동적 검색 없이 메서드 이름 쿼리만 쓴다.
 */
public interface CodeValueRepository extends JpaRepository<CodeValue, Long> {

    /**
     * 그룹의 전체 코드값을 정렬 순서, 같으면 ID 오름차순으로 조회한다.
     * 그룹 삭제 시에도 이 메서드로 불러와 값마다 삭제 이력을 남기고 엔티티 단위로 삭제한다.
     */
    List<CodeValue> findAllByGroupIdOrderBySortOrderAscIdAsc(Long groupId);

    /** 경로의 그룹에 속한 코드값만 찾는다. 다른 그룹의 코드값 ID면 빈 값이 되어 404로 응답한다. */
    Optional<CodeValue> findByIdAndGroupId(Long id, Long groupId);

    /** 등록 시 그룹 안에서 코드값 중복 확인. 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByGroupIdAndCodeValue(Long groupId, String codeValue);

    /** 수정 시 그룹 안에서 코드값 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByGroupIdAndCodeValueAndIdNot(Long groupId, String codeValue, Long id);
}
