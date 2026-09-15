package com.step.dictionary.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 표준/메타 엔티티가 상속하는 생성·수정 일시 공통 부모.
 *
 * <p>{@link MappedSuperclass}라 자체 테이블은 없고, 자식 엔티티 테이블에 {@code created_at}, {@code updated_at} 컬럼으로 들어간다.
 * 값은 {@link AuditingEntityListener}가 insert/update 직전에 채우므로 코드에서 직접 설정하지 않는다.
 * ({@code JpaConfig}의 {@code @EnableJpaAuditing}이 켜져 있어야 동작한다.)
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    /** 생성 일시. 최초 저장 시 한 번 채워지고 이후 update 문에서 제외된다. */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 최종 수정 일시. 저장·변경 감지로 update가 flush될 때마다 갱신된다.
     * 서비스의 수정 메서드가 응답 전에 {@code flush()}를 호출하는 이유는 이 값을 응답에 반영하기 위해서다.
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
