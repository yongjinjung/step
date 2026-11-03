package com.step.dictionary.standard.domain;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.standard.domain.dto.DomainSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 표준 도메인 리포지토리 ({@link DomainMapper} 사용). 조회한 도메인에는 분류어 단어가 함께 채워져 있다.
 * 엔티티를 바꾼 뒤에는 {@link #save}를 호출해야 DB에 반영된다.
 */
@Repository
public class DomainRepository {

    private final DomainMapper mapper;

    public DomainRepository(DomainMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 수정한다. */
    public Domain save(Domain domain) {
        if (domain.getId() == null) {
            mapper.insert(domain);
        } else {
            mapper.update(domain);
        }
        return domain;
    }

    public Optional<Domain> findById(Long id) {
        return mapper.findById(id);
    }

    /** 도메인명으로 찾는다. 엑셀 업로드에서 용어의 도메인을 이름으로 지정할 때 쓴다. */
    public Optional<Domain> findByName(String name) {
        return mapper.findByName(name);
    }

    /** 등록 시 도메인명 중복 확인. */
    public boolean existsByName(String name) {
        return mapper.countByName(name, null) > 0;
    }

    /** 수정 시 도메인명 중복 확인. 자기 자신(id)은 제외한다. */
    public boolean existsByNameAndIdNot(String name, Long id) {
        return mapper.countByName(name, id) > 0;
    }

    /** 단어가 도메인의 분류어로 쓰이는지. 단어 삭제·분류어 해제 시 참조 검사에 쓴다. */
    public boolean existsByClassifierWordId(Long wordId) {
        return mapper.countByClassifierWordId(wordId) > 0;
    }

    /** 조건에 맞는 도메인을 도메인명 오름차순으로 페이지 조회한다. */
    public Page<Domain> search(DomainSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }

    public void delete(Domain domain) {
        mapper.deleteById(domain.getId());
    }
}
