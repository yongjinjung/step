package com.step.dictionary.pharmacy;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 하위 품목 요청 검사. 조제·검수는 앞 단계의 품목을 <b>빠짐없이, 한 번씩</b> 다뤄야 한다.
 * (예: 처방 품목이 3개면 조제 품목도 그 3개를 하나씩 가져야 한다.)
 */
public final class PharmacyItems {

    /** 인스턴스 생성 방지. */
    private PharmacyItems() {
    }

    /**
     * 요청 품목 ID가 대상 품목 ID와 정확히 같은 집합인지(중복·누락·다른 대상의 품목 없음) 검사한다.
     *
     * @param requestedIds 요청에 들어온 품목 ID (순서대로)
     * @param expectedIds  앞 단계 품목 ID
     * @param errorCode    어긋날 때 던질 오류
     */
    public static void requireExactly(List<Long> requestedIds, Collection<Long> expectedIds, ErrorCode errorCode) {
        Set<Long> requested = new HashSet<>(requestedIds);
        if (requested.size() != requestedIds.size() || !requested.equals(new HashSet<>(expectedIds))) {
            throw new BusinessException(errorCode);
        }
    }

    /** 요청 품목 ID에 중복이 없고, 모두 대상 품목에 속하는지 검사한다 (일부만 다뤄도 되는 반납용). */
    public static void requireSubset(List<Long> requestedIds, Collection<Long> expectedIds, ErrorCode errorCode) {
        Set<Long> requested = new HashSet<>(requestedIds);
        if (requested.size() != requestedIds.size() || !new HashSet<>(expectedIds).containsAll(requested)) {
            throw new BusinessException(errorCode);
        }
    }
}
