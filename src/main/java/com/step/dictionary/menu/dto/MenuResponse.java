package com.step.dictionary.menu.dto;

import com.step.dictionary.menu.Menu;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 메뉴 응답. 상위 메뉴는 {@code children}에 하위 메뉴를 담고, 하위 메뉴는 빈 목록이다.
 *
 * @param id        메뉴 ID
 * @param parentId  상위 메뉴 ID (상위 메뉴 자신이면 null)
 * @param name      메뉴명
 * @param path      화면 경로 (상위 메뉴는 null)
 * @param icon      아이콘 이름 (없으면 null)
 * @param sortOrder 정렬 순서
 * @param used      사용 여부
 * @param adminOnly 관리자 전용 여부
 * @param children  하위 메뉴 (정렬 순서대로). 하위 메뉴의 응답에서는 항상 빈 목록
 * @param createdAt 생성 일시
 * @param updatedAt 최종 수정 일시
 */
public record MenuResponse(
        Long id,
        Long parentId,
        String name,
        String path,
        String icon,
        int sortOrder,
        boolean used,
        boolean adminOnly,
        List<MenuResponse> children,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 하위 메뉴 없이 변환한다. */
    public static MenuResponse from(Menu menu) {
        return of(menu, List.of());
    }

    /**
     * 하위 메뉴와 함께 변환한다.
     * 상위 메뉴를 읽으므로(부모 ID) 트랜잭션 안에서 호출하거나 상위 메뉴를 함께 조회해 둬야 한다.
     */
    public static MenuResponse of(Menu menu, List<MenuResponse> children) {
        return new MenuResponse(
                menu.getId(),
                menu.isTop() ? null : menu.getParent().getId(),
                menu.getName(),
                menu.getPath(),
                menu.getIcon(),
                menu.getSortOrder(),
                menu.isUsed(),
                menu.isAdminOnly(),
                children,
                menu.getCreatedAt(),
                menu.getUpdatedAt());
    }
}
