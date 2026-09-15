package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.history.HistoryTargetType;
import com.step.dictionary.standard.domain.DataType;
import com.step.dictionary.standard.domain.Domain;

/**
 * 표준 도메인 스냅샷.
 *
 * @param id                 도메인 ID
 * @param name               도메인명
 * @param classifierWordId   분류어 단어 ID
 * @param classifierWordName 분류어 단어명 (읽기용)
 * @param dataType           데이터 타입
 * @param dataLength         길이
 * @param dataScale          소수점 자리수
 * @param description        설명
 */
public record DomainSnapshot(Long id, String name, Long classifierWordId, String classifierWordName,
        DataType dataType, Integer dataLength, Integer dataScale, String description) implements HistorySnapshot {

    /** 엔티티의 현재 값으로 스냅샷을 뜬다. 분류어 단어명을 읽으므로 트랜잭션 안에서 호출한다. */
    public static DomainSnapshot from(Domain domain) {
        return new DomainSnapshot(domain.getId(), domain.getName(), domain.getClassifierWord().getId(),
                domain.getClassifierWord().getName(), domain.getDataType(), domain.getDataLength(),
                domain.getDataScale(), domain.getDescription());
    }

    @Override
    @JsonIgnore
    public HistoryTargetType targetType() {
        return HistoryTargetType.DOMAIN;
    }
}
