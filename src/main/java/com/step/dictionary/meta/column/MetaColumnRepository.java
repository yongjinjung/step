package com.step.dictionary.meta.column;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 컬럼 정의 리포지토리.
 * 기본 CRUD는 {@link JpaRepository}, 비표준 검색은 {@link MetaColumnRepositoryCustom}이 맡는다.
 */
public interface MetaColumnRepository extends JpaRepository<MetaColumn, Long>, MetaColumnRepositoryCustom {

    /**
     * 테이블의 전체 컬럼을 순서(seq), 같은 순서면 ID 오름차순으로 조회한다.
     * 검출에 용어와 도메인이 필요하므로 함께 가져온다.
     * {@code @EntityGraph}는 지정한 연관을 left join fetch로 가져와 컬럼마다 용어/도메인을 따로 조회하는 N+1을 막는다.
     * 테이블 삭제 시에도 이 메서드로 컬럼을 불러와 컬럼별 삭제 이력을 남기고 엔티티 단위로 삭제한다.
     */
    @EntityGraph(attributePaths = {"term", "term.domain"})
    List<MetaColumn> findAllByTableIdOrderBySeqAscIdAsc(Long tableId);

    /**
     * 여러 테이블의 컬럼을 한 번에 조회한다. 엑셀 다운로드에서 테이블마다 컬럼을 따로 조회하는 N+1을 막는다.
     * 테이블 물리명, 매핑 용어명, 비표준 검출에 필요한 연관을 함께 가져온다. 빈 목록을 넘기지 않는다.
     */
    @EntityGraph(attributePaths = {"table", "term", "term.domain"})
    List<MetaColumn> findAllByTableIdInOrderBySeqAscIdAsc(Collection<Long> tableIds);

    /** 경로의 테이블에 속한 컬럼만 찾는다. 다른 테이블의 컬럼 ID면 빈 값이 되어 404로 응답한다. */
    Optional<MetaColumn> findByIdAndTableId(Long id, Long tableId);

    /** 등록 시 테이블 안에서 물리명 중복 확인. 대문자로 정규화한 값을 넘겨야 한다. */
    boolean existsByTableIdAndPhysicalName(Long tableId, String physicalName);

    /** 수정 시 테이블 안에서 물리명 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByTableIdAndPhysicalNameAndIdNot(Long tableId, String physicalName, Long id);

    /** 이 용어에 매핑된 컬럼이 있는지 확인한다. 용어 삭제 차단(TERM_IN_USE)에 쓴다. */
    boolean existsByTermId(Long termId);
}
