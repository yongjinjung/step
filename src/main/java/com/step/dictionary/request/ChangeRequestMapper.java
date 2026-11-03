package com.step.dictionary.request;

import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;

/** 표준 데이터 변경 신청 SQL 매퍼. 조회 결과에 신청자·검토자(역할 제외)를 함께 채운다. */
@Mapper
public interface ChangeRequestMapper {

    Optional<ChangeRequest> findById(Long id);

    long countByTargetAndStatus(@Param("targetType") TargetType targetType, @Param("targetId") Long targetId,
            @Param("status") RequestStatus status);

    /** 조건 검색 (최신 신청 먼저 = ID 내림차순). */
    List<ChangeRequest> search(@Param("c") ChangeRequestSearchCondition condition, @Param("p") Pageable pageable);

    long count(@Param("c") ChangeRequestSearchCondition condition);

    void insert(ChangeRequest request);

    /** 처리 결과(상태, 검토자, 의견, 처리 일시, 반영된 대상 ID)만 바뀐다. */
    void update(ChangeRequest request);
}
