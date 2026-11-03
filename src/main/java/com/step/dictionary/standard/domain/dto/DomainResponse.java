package com.step.dictionary.standard.domain.dto;

import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;
import java.time.LocalDateTime;

/**
 * 도메인 응답.
 *
 * @param id                 도메인 ID
 * @param name               도메인명
 * @param classifierWordId   분류어 단어 ID
 * @param classifierWordName 분류어 단어명 (화면 표시용)
 * @param dataType           데이터 타입
 * @param dataLength         길이 (null 가능)
 * @param dataScale          소수점 자리수 (DECIMAL은 0 이상, 나머지는 null)
 * @param description        설명 (null 가능)
 * @param createdAt          생성 일시
 * @param updatedAt          최종 수정 일시
 */
public record DomainResponse(
        Long id,
        String name,
        Long classifierWordId,
        String classifierWordName,
        DataType dataType,
        Integer dataLength,
        Integer dataScale,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /**
     * 엔티티를 응답으로 변환한다.
     * 분류어 단어는 조회 쿼리에서 조인해 채워 둔다.
     */
    public static DomainResponse from(Domain domain) {
        return new DomainResponse(
                domain.getId(),
                domain.getName(),
                domain.getClassifierWord().getId(),
                domain.getClassifierWord().getName(),
                domain.getDataType(),
                domain.getDataLength(),
                domain.getDataScale(),
                domain.getDescription(),
                domain.getCreatedAt(),
                domain.getUpdatedAt());
    }
}
