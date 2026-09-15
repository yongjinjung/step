package com.step.dictionary.request;

/**
 * 신청 종류. 종류마다 대상 ID와 신청 내용(payload)의 필요 여부가 다르다.
 *
 * <table>
 *   <tr><th>종류</th><th>targetId</th><th>payload</th></tr>
 *   <tr><td>CREATE</td><td>없어야 함 (승인 시 채워짐)</td><td>필수</td></tr>
 *   <tr><td>UPDATE</td><td>필수</td><td>필수 (전체 교체 값)</td></tr>
 *   <tr><td>DELETE</td><td>필수</td><td>없어야 함</td></tr>
 * </table>
 */
public enum RequestType {

    /** 등록. */
    CREATE,
    /** 수정. 기존 수정 API(PUT)와 같이 전체 값을 교체한다. */
    UPDATE,
    /** 삭제. */
    DELETE;

    /** 이 종류가 대상 ID를 신청 시점에 요구하는지. */
    public boolean requiresTargetId() {
        return this != CREATE;
    }

    /** 이 종류가 신청 내용을 요구하는지. */
    public boolean requiresPayload() {
        return this != DELETE;
    }
}
