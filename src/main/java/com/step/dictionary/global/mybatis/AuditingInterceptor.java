package com.step.dictionary.global.mybatis;

import com.step.dictionary.global.entity.BaseTimeEntity;
import com.step.dictionary.global.entity.CreatedAtAware;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

/**
 * 생성·수정 일시 자동 기록 (JPA 감사 기능을 대신한다).
 *
 * <p>MyBatis insert/update 문이 실행되기 직전에 파라미터를 보고
 * <ul>
 *   <li>insert: {@link CreatedAtAware#markCreated} (BaseTimeEntity면 생성·수정 일시 모두)</li>
 *   <li>update: {@link BaseTimeEntity#markUpdated}</li>
 * </ul>
 * 를 호출한다. 파라미터가 {@code @Param}으로 여러 개면 그 안의 객체를 모두 본다.
 * DB 컬럼이 {@code DATETIME(6)}이라 마이크로초로 잘라서, 저장 직후 응답과 다시 조회한 값이 같게 한다.
 */
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class AuditingInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType command = statement.getSqlCommandType();
        if (command == SqlCommandType.INSERT || command == SqlCommandType.UPDATE) {
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
            for (Object target : targets(invocation.getArgs()[1])) {
                if (command == SqlCommandType.INSERT && target instanceof CreatedAtAware created) {
                    created.markCreated(now);
                } else if (command == SqlCommandType.UPDATE && target instanceof BaseTimeEntity entity) {
                    entity.markUpdated(now);
                }
            }
        }
        return invocation.proceed();
    }

    /** 파라미터 객체. {@code @Param} 묶음(Map)이면 그 값들, 컬렉션이면 원소들 (같은 객체는 한 번만). */
    private static Collection<Object> targets(Object parameter) {
        Set<Object> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        if (parameter instanceof Map<?, ?> map) {
            map.values().forEach(value -> addTarget(targets, value));
        } else {
            addTarget(targets, parameter);
        }
        return targets;
    }

    private static void addTarget(Set<Object> targets, Object value) {
        if (value instanceof Collection<?> collection) {
            targets.addAll(collection);
        } else if (value != null) {
            targets.add(value);
        }
    }
}
