package com.step.dictionary.code.group;

import com.step.dictionary.code.group.dto.CodeGroupSearchCondition;
import com.step.dictionary.global.mybatis.Pages;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 공통 코드 그룹 리포지토리 ({@link CodeGroupMapper} 사용). 엔티티를 바꾼 뒤에는 {@link #save}를 호출한다. */
@Repository
public class CodeGroupRepository {

    private final CodeGroupMapper mapper;

    public CodeGroupRepository(CodeGroupMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. */
    public CodeGroup save(CodeGroup group) {
        if (group.getId() == null) {
            mapper.insert(group);
        } else {
            mapper.update(group);
        }
        return group;
    }

    public Optional<CodeGroup> findById(Long id) {
        return mapper.findById(id);
    }

    /** 등록 시 코드ID 중복 확인. 대문자로 정규화한 값을 넘긴다. */
    public boolean existsByCodeId(String codeId) {
        return mapper.countByCodeId(codeId, null) > 0;
    }

    /** 수정 시 코드ID 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByCodeIdAndIdNot(String codeId, Long id) {
        return mapper.countByCodeId(codeId, id) > 0;
    }

    /** 조건에 맞는 코드 그룹을 코드ID 오름차순으로 페이지 조회한다. */
    public Page<CodeGroup> search(CodeGroupSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }

    public void delete(CodeGroup group) {
        mapper.deleteById(group.getId());
    }
}
