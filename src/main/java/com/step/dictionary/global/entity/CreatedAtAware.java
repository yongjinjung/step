package com.step.dictionary.global.entity;

import java.time.LocalDateTime;

/**
 * insert 직전에 생성 일시를 받는 객체.
 *
 * <p>{@link com.step.dictionary.global.mybatis.AuditingInterceptor}가 insert 문의 파라미터가 이 타입이면
 * {@link #markCreated}를 호출한다. 수정 일시가 없는 테이블(역할별 메뉴 권한, 사용자별 메뉴)도 이것만 구현한다.
 */
public interface CreatedAtAware {

    /** 생성 일시를 채운다. 코드에서 직접 부르지 않는다. */
    void markCreated(LocalDateTime now);
}
