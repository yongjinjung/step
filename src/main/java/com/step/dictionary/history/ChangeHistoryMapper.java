package com.step.dictionary.history;

import com.step.dictionary.history.dto.ChangeHistorySearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 변경 이력 SQL 매퍼. 조회 결과에 변경자(역할 제외)를 함께 채운다. 이력은 수정·삭제하지 않는다. */
@Mapper
public interface ChangeHistoryMapper {

    Optional<ChangeHistory> findById(Long id);

    /** 조건 검색 (최신 이력 먼저 = ID 내림차순). */
    List<ChangeHistory> search(@Param("c") ChangeHistorySearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") ChangeHistorySearchCondition condition);

    void insert(ChangeHistory history);
}
