package com.step.dictionary.global.entity;

import java.time.LocalDateTime;

/**
 * 생성·수정 일시를 가진 도메인 객체의 공통 부모. 자식 테이블의 {@code created_at}, {@code updated_at} 컬럼과 같다.
 *
 * <p>값은 {@link com.step.dictionary.global.mybatis.AuditingInterceptor}가 MyBatis insert/update 직전에 채우므로
 * 코드에서 직접 설정하지 않는다. 서비스가 엔티티를 바꾼 뒤 리포지토리의 {@code save()}(update)를 호출하면
 * 이 객체의 {@code updatedAt}도 함께 바뀌어 응답에 그대로 쓸 수 있다.
 */
public abstract class BaseTimeEntity implements CreatedAtAware {

    /** 생성 일시. insert 때 한 번 채워지고 update 문에는 넣지 않는다. */
    private LocalDateTime createdAt;

    /** 최종 수정 일시. insert와 update 때마다 갱신된다. */
    private LocalDateTime updatedAt;

    @Override
    public void markCreated(LocalDateTime now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 수정 일시를 채운다. update 직전에 인터셉터가 호출한다. */
    public void markUpdated(LocalDateTime now) {
        this.updatedAt = now;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
