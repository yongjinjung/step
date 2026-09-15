package com.step.dictionary.request;

/**
 * 신청 상태. {@link #REQUESTED}에서만 다른 상태로 바뀔 수 있고, 한 번 처리된 신청은 되돌리지 않는다.
 */
public enum RequestStatus {

    /** 처리 대기. 신청 직후 상태. */
    REQUESTED,
    /** 승인됨. 대상에 반영이 끝났다. */
    APPROVED,
    /** 반려됨. 대상은 바뀌지 않았다. */
    REJECTED,
    /** 신청자가 취소함. */
    CANCELED
}
