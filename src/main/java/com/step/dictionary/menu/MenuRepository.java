package com.step.dictionary.menu;

import com.step.dictionary.global.mybatis.InClause;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 화면 메뉴 리포지토리 ({@link MenuMapper} 사용). 조회한 메뉴에는 상위 메뉴가 한 단계 채워져 있다.
 * 엔티티를 바꾼 뒤에는 {@link #save}를 호출한다.
 */
@Repository
public class MenuRepository {

    private final MenuMapper mapper;

    public MenuRepository(MenuMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. */
    public Menu save(Menu menu) {
        if (menu.getId() == null) {
            mapper.insert(menu);
        } else {
            mapper.update(menu);
        }
        return menu;
    }

    public Optional<Menu> findById(Long id) {
        return mapper.findById(id);
    }

    /** 전체 메뉴를 정렬 순서, 같으면 ID 오름차순으로. 화면 표시용 트리를 만들 때 쓴다. */
    public List<Menu> findAllByOrderBySortOrderAscIdAsc() {
        return mapper.findAll();
    }

    /** 여러 ID를 한 번에 찾는다. 없는 ID는 결과에서 빠진다. */
    public List<Menu> findAllById(Collection<Long> ids) {
        return InClause.query(ids, mapper::findAllById);
    }

    /** 메뉴 수. 기본 메뉴 생성 여부(비어 있을 때만)를 정한다. */
    public long count() {
        return mapper.count();
    }

    /** 등록 시 화면 경로 중복 확인. */
    public boolean existsByPath(String path) {
        return mapper.countByPath(path, null) > 0;
    }

    /** 수정 시 화면 경로 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByPathAndIdNot(String path, Long id) {
        return mapper.countByPath(path, id) > 0;
    }

    /** 하위 메뉴가 있는지. 하위가 있는 상위 메뉴는 삭제할 수 없다. */
    public boolean existsByParentId(Long parentId) {
        return mapper.countByParentId(parentId) > 0;
    }

    /** 메뉴만 지운다. 걸린 권한(역할별·사용자별)은 서비스가 먼저 지워야 한다 (FK). */
    public void delete(Menu menu) {
        mapper.deleteById(menu.getId());
    }
}
