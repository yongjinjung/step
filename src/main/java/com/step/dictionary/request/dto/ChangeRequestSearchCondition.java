package com.step.dictionary.request.dto;

import com.step.dictionary.request.RequestStatus;
import com.step.dictionary.request.RequestType;
import com.step.dictionary.request.TargetType;

/**
 * 신청 검색 조건. 쿼리 파라미터가 record 생성자로 바인딩된다. 모든 조건은 선택이다.
 *
 * @param status      상태 (null이면 전체)
 * @param targetType  대상 종류 (null이면 전체)
 * @param requestType 신청 종류 (null이면 전체)
 * @param requester   신청자 아이디 (대소문자 구분 없음). USER가 조회하면 서비스가 본인 아이디로 덮어쓴다
 */
public record ChangeRequestSearchCondition(
        RequestStatus status, TargetType targetType, RequestType requestType, String requester) {

    /** 신청자만 바꾼 조건을 만든다. USER의 조회 범위를 본인 신청으로 제한할 때 쓴다. */
    public ChangeRequestSearchCondition withRequester(String username) {
        return new ChangeRequestSearchCondition(status, targetType, requestType, username);
    }
}
