package com.step.dictionary.request.dto;

import com.step.dictionary.request.RequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 변경 신청 요청 본문. 대상 종류별 요청 DTO를 신청 내용으로 감싼다.
 *
 * <pre>
 * {"requestType": "UPDATE", "targetId": 3, "reason": "영문명 오타 수정",
 *  "payload": {"name": "고객", "abbreviation": "CUST", "englishName": "Customer", "description": null, "classifier": false}}
 * </pre>
 *
 * <p>{@code @Valid}가 신청 내용(payload)의 실제 타입(WordRequest 등) 검증 규칙까지 적용하므로,
 * 필드 오류는 {@code payload.abbreviation}처럼 경로가 붙어 나온다.
 * 신청 종류와 대상 ID/신청 내용의 조합 검사는 서비스에서 한다 (400 INVALID_CHANGE_REQUEST).
 *
 * @param requestType 신청 종류. 필수
 * @param targetId    대상 ID. 수정/삭제는 필수, 등록은 비워야 한다
 * @param payload     신청 내용. 등록/수정은 필수, 삭제는 비워야 한다
 * @param reason      신청 사유. 선택, 최대 1000자
 * @param <T>         대상별 요청 DTO (WordRequest, DomainRequest, TermRequest)
 */
public record ChangeRequestForm<T>(
        @NotNull
        RequestType requestType,

        Long targetId,

        @Valid
        T payload,

        @Size(max = 1000)
        String reason
) {
}
