package com.step.dictionary.request;

import com.step.dictionary.global.error.ErrorCode;

/**
 * 신청 대상 종류. 표준 데이터 중 신청/승인을 거치는 세 가지다.
 * 테이블/컬럼 정의는 USER도 직접 쓸 수 있으므로 신청 대상이 아니다.
 */
public enum TargetType {

    /** 표준 단어. */
    WORD(ErrorCode.WORD_NOT_FOUND),
    /** 표준 도메인. */
    DOMAIN(ErrorCode.DOMAIN_NOT_FOUND),
    /** 표준 용어. */
    TERM(ErrorCode.TERM_NOT_FOUND);

    /** 수정/삭제 신청의 대상이 없을 때 쓰는 오류. 대상별 기존 404 코드를 그대로 쓴다. */
    private final ErrorCode notFoundError;

    TargetType(ErrorCode notFoundError) {
        this.notFoundError = notFoundError;
    }

    public ErrorCode getNotFoundError() {
        return notFoundError;
    }
}
