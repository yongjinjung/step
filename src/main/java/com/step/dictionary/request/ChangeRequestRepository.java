package com.step.dictionary.request;

import com.step.dictionary.global.mybatis.Pages;
import com.step.dictionary.request.dto.ChangeRequestSearchCondition;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * 변경 신청 리포지토리 ({@link ChangeRequestMapper} 사용). 조회한 신청에는 신청자·검토자가 채워져 있다.
 * 처리(승인·반려·취소) 후에는 {@link #save}를 호출한다. 신청은 삭제하지 않는다.
 */
@Repository
public class ChangeRequestRepository {

    private final ChangeRequestMapper mapper;

    public ChangeRequestRepository(ChangeRequestMapper mapper) {
        this.mapper = mapper;
    }

    /** ID가 없으면 등록, 있으면 처리 결과를 저장한다. */
    public ChangeRequest save(ChangeRequest request) {
        if (request.getId() == null) {
            mapper.insert(request);
        } else {
            mapper.update(request);
        }
        return request;
    }

    /** 신청자·검토자와 함께 조회한다. */
    public Optional<ChangeRequest> findWithUsersById(Long id) {
        return mapper.findById(id);
    }

    /** 같은 대상에 대기 중인 신청이 있는지. 중복 신청을 막는 데 쓴다. */
    public boolean existsByTargetTypeAndTargetIdAndStatus(TargetType targetType, Long targetId,
            RequestStatus status) {
        return mapper.countByTargetAndStatus(targetType, targetId, status) > 0;
    }

    /** 조건에 맞는 신청을 최신순(ID 내림차순)으로 페이지 조회한다. */
    public Page<ChangeRequest> search(ChangeRequestSearchCondition condition, Pageable pageable) {
        return Pages.of(mapper.search(condition, pageable), pageable, () -> mapper.count(condition));
    }
}
