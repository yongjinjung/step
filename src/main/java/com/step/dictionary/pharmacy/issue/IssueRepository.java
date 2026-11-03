package com.step.dictionary.pharmacy.issue;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.pharmacy.issue.dto.IssueSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** 불출 리포지토리 ({@link IssueMapper} 사용). 불출과 품목을 한 묶음으로 등록·조회한다(수정 없음). */
@Repository
public class IssueRepository {

    private final IssueMapper mapper;

    public IssueRepository(IssueMapper mapper) {
        this.mapper = mapper;
    }

    /** 불출을 품목과 함께 등록한다. */
    public Issue save(Issue issue) {
        mapper.insert(issue);
        issue.getItems().forEach(item -> mapper.insertItem(issue.getId(), item));
        return issue;
    }

    /** 불출을 품목과 함께 조회한다. */
    public Optional<Issue> findById(Long id) {
        return mapper.findById(id).map(issue -> {
            issue.loadItems(mapper.findItemsByIssueId(id));
            return issue;
        });
    }

    /** 조건에 맞는 불출을 불출 일시 최신순으로 페이지 조회한다 (품목 제외). */
    public Page<Issue> search(IssueSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
