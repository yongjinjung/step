package com.step.dictionary.pharmacy.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 불출 등록 요청. 품목은 받지 않는다(조제 품목을 그대로 불출한다). 불출자와 불출 일시는 서버가 채운다.
 *
 * @param dispensingId    불출할 조제 ID (검수 합격한 조제)
 * @param destinationCode 불출처 코드. 필수, 최대 20자 (저장 시 대문자). 예: W71
 * @param destinationName 불출처 이름. 필수, 최대 100자. 예: 71병동
 * @param receiverName    수령자 이름. 필수, 최대 100자
 * @param remark          비고. 선택, 최대 1000자
 */
public record IssueRequest(
        @NotNull Long dispensingId,
        @NotBlank @Size(max = 20) String destinationCode,
        @NotBlank @Size(max = 100) String destinationName,
        @NotBlank @Size(max = 100) String receiverName,
        @Size(max = 1000) String remark
) {
}
