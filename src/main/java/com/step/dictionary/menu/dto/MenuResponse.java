package com.step.dictionary.menu.dto;

import com.step.dictionary.menu.Menu;
import com.step.dictionary.role.PermissionAction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 메뉴 응답. 상위 메뉴는 {@code children}에 하위 메뉴를 담고, 하위 메뉴는 빈 목록이다.
 *
 * <p>{@code canRead}~{@code canDelete}는 <b>이 응답을 받는 사용자</b>가 그 메뉴에서 할 수 있는 일이다
 * (역할 권한의 합집합). 화면은 이 값으로 버튼을 보여줄지 정한다. 서버도 같은 권한으로 요청을 검사한다.
 *
 * @param id        메뉴 ID
 * @param parentId  상위 메뉴 ID (상위 메뉴 자신이면 null)
 * @param name      메뉴명
 * @param path      화면 경로 (상위 메뉴는 null)
 * @param icon      아이콘 이름 (없으면 null)
 * @param sortOrder 정렬 순서
 * @param used      사용 여부
 * @param canRead   조회 권한
 * @param canCreate 등록 권한
 * @param canUpdate 수정 권한
 * @param canDelete 삭제 권한
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
        boolean canRead,
        boolean canCreate,
        boolean canUpdate,
        boolean canDelete,
        List<MenuResponse> children,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    /** 하위 메뉴와 권한 없이 변환한다 (등록·수정 응답). */
    public static MenuResponse from(Menu menu) {
        return of(menu, List.of(), Set.of());
    }

    /**
     * 하위 메뉴·권한과 함께 변환한다.
     * 상위 메뉴를 읽으므로(부모 ID) 트랜잭션 안에서 호출하거나 상위 메뉴를 함께 조회해 둬야 한다.
     *
     * @param actions 이 응답을 받는 사용자가 그 메뉴에서 할 수 있는 동작
     */
    public static MenuResponse of(Menu menu, List<MenuResponse> children, Set<PermissionAction> actions) {
        return new MenuResponse(
                menu.getId(),
                menu.isTop() ? null : menu.getParent().getId(),
                menu.getName(),
                menu.getPath(),
                menu.getIcon(),
                menu.getSortOrder(),
                menu.isUsed(),
                actions.contains(PermissionAction.READ),
                actions.contains(PermissionAction.CREATE),
                actions.contains(PermissionAction.UPDATE),
                actions.contains(PermissionAction.DELETE),
                children,
                menu.getCreatedAt(),
                menu.getUpdatedAt());
    }
}
