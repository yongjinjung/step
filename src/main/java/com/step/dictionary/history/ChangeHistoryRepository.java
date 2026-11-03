package com.step.dictionary.history;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 변경 이력 리포지토리 ({@link ChangeHistoryMapper} 사용). 이력은 등록과 조회만 한다. */
@Repository
public class ChangeHistoryRepository {

    private final ChangeHistoryMapper mapper;

    public ChangeHistoryRepository(ChangeHistoryMapper mapper) {
        this.mapper = mapper;
    }

    /** 이력을 등록한다. 생성된 ID가 채워진다. */
    public ChangeHistory save(ChangeHistory history) {
        mapper.insert(history);
        return history;
    }

    /** 변경자와 함께 조회한다. */
    public Optional<ChangeHistory> findWithUserById(Long id) {
        return mapper.findById(id);
    }

    /** 조건에 맞는 이력을 최신순(ID 내림차순)으로 페이지 조회한다. */
    public Page<ChangeHistory> search(ChangeHistorySearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
