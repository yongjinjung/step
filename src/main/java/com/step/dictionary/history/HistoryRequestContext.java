package com.step.dictionary.history;

import java.util.function.Supplier;

/**
 * "지금 반영 중인 신청"을 이력 기록기에 알려주는 스레드 단위 컨텍스트.
 *
 * <p>신청 승인은 기존 서비스({@code WordService} 등)를 그대로 호출해 반영한다. 서비스 메서드에 신청 ID 파라미터를
 * 추가하지 않고도 {@link ChangeHistoryRecorder}가 이력에 신청 ID를 남길 수 있도록, 승인하는 동안만 값을 걸어 둔다.
 * 요청 하나는 한 스레드에서 처리되므로 ThreadLocal로 충분하다. 끝나면 반드시 원래 값으로 되돌린다.
 */
public final class HistoryRequestContext {

    private static final ThreadLocal<Long> REQUEST_ID = new ThreadLocal<>();

    /** 인스턴스 생성 방지. */
    private HistoryRequestContext() {
    }

    /**
     * 신청 ID를 걸어 둔 채로 작업을 실행한다. 작업 안에서 기록되는 이력에는 이 신청 ID가 들어간다.
     * 예외가 나도 finally에서 이전 값으로 되돌려, 스레드 풀이 스레드를 재사용할 때 값이 새지 않게 한다.
     */
    public static <T> T callWithRequest(Long requestId, Supplier<T> action) {
        Long previous = REQUEST_ID.get();
        REQUEST_ID.set(requestId);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                REQUEST_ID.remove();
            } else {
                REQUEST_ID.set(previous);
            }
        }
    }

    /** 현재 반영 중인 신청 ID. 신청 승인이 아니면 null. */
    public static Long currentRequestId() {
        return REQUEST_ID.get();
    }
}
