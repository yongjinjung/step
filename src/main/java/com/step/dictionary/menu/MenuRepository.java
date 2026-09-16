package com.step.dictionary.menu;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 메뉴 리포지토리.
 * 메뉴는 수가 적어 트리를 만들 때 전체를 한 번에 읽고 메모리에서 묶는다 (동적 검색 없음).
 */
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /** 전체 메뉴를 정렬 순서, 같으면 ID 오름차순으로 읽는다. 상위·하위가 섞여 나오며 서비스가 트리로 묶는다. */
    List<Menu> findAllByOrderBySortOrderAscIdAsc();

    /** 등록 시 경로 중복 확인. {@link Menu#normalizePath} 를 거친 값을 넘겨야 한다. */
    boolean existsByPath(String path);

    /** 수정 시 경로 중복 확인. 자기 자신(id)은 제외한다. */
    boolean existsByPathAndIdNot(String path, Long id);

    /** 하위 메뉴가 있는지. 삭제 차단(MENU_HAS_CHILDREN)과 2단 제한 검사에 쓴다. */
    boolean existsByParentId(Long parentId);
}
