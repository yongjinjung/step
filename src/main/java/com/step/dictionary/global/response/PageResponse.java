package com.step.dictionary.global.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 목록 조회 공통 응답.
 *
 * <p>Spring {@link Page}를 그대로 직렬화하면 {@code pageable}, {@code sort} 등 내부 구조가 노출되고
 * 버전에 따라 JSON 모양이 바뀔 수 있다. 그래서 필요한 값만 골라 고정된 형식으로 내려준다.
 *
 * @param content       현재 페이지 항목
 * @param page          현재 페이지 번호 (0부터 시작)
 * @param size          요청한 페이지 크기
 * @param totalElements 조건에 맞는 전체 항목 수
 * @param totalPages    전체 페이지 수
 * @param <T>           항목 타입 (응답 DTO)
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    /** 엔티티를 응답 DTO로 {@code map()}한 Page를 받아 변환한다. */
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
