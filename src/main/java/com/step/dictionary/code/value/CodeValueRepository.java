package com.step.dictionary.code.value;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** 공통 코드값 리포지토리 ({@link CodeValueMapper} 사용). 엔티티를 바꾼 뒤에는 {@link #save}를 호출한다. */
@Repository
public class CodeValueRepository {

    private final CodeValueMapper mapper;

    public CodeValueRepository(CodeValueMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. */
    public CodeValue save(CodeValue value) {
        if (value.getId() == null) {
            mapper.insert(value);
        } else {
            mapper.update(value);
        }
        return value;
    }

    /** 그룹의 코드값 전체를 정렬 순서(같으면 ID) 오름차순으로. */
    public List<CodeValue> findAllByGroupIdOrderBySortOrderAscIdAsc(Long groupId) {
        return mapper.findAllByGroupId(groupId);
    }

    /** 경로의 그룹에 속한 코드값만 찾는다. 다른 그룹의 코드값 ID로 접근하면 비어 있다. */
    public Optional<CodeValue> findByIdAndGroupId(Long id, Long groupId) {
        return mapper.findByIdAndGroupId(id, groupId);
    }

    /** 등록 시 그룹 안 코드값 중복 확인. 대문자로 정규화한 값을 넘긴다. */
    public boolean existsByGroupIdAndCodeValue(Long groupId, String codeValue) {
        return mapper.countByGroupIdAndCodeValue(groupId, codeValue, null) > 0;
    }

    /** 수정 시 그룹 안 코드값 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByGroupIdAndCodeValueAndIdNot(Long groupId, String codeValue, Long id) {
        return mapper.countByGroupIdAndCodeValue(groupId, codeValue, id) > 0;
    }

    public void delete(CodeValue value) {
        mapper.deleteById(value.getId());
    }

    public void deleteAll(Collection<CodeValue> values) {
        values.forEach(this::delete);
    }
}
