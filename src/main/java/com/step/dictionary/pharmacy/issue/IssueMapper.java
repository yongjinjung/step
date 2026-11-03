package com.step.dictionary.pharmacy.issue;

import com.step.dictionary.pharmacy.issue.dto.IssueSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/**
 * 불출 SQL 매퍼. 불출 행은 조제·처방과 함께, 품목은 조제 품목·처방 품목과 함께 읽는다.
 * XML에는 반납 매퍼가 쓰는 불출·불출 품목 결과 매핑과 컬럼 조각도 있다.
 */
@Mapper
public interface IssueMapper {

    Optional<Issue> findById(Long id);

    /** 불출의 품목 (ID 순 = 조제 품목 순). */
    List<IssueItem> findItemsByIssueId(Long issueId);

    /** 조건 검색 (불출 일시 최신순, 같으면 ID 내림차순). 품목은 읽지 않는다. */
    List<Issue> search(@Param("c") IssueSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") IssueSearchCondition condition);

    void insert(Issue issue);

    void insertItem(@Param("issueId") Long issueId, @Param("item") IssueItem item);
}
