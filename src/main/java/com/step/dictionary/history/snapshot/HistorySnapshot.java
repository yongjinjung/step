package com.step.dictionary.history.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.step.dictionary.history.HistoryTargetType;

/**
 * 변경 이력에 저장하는 대상 스냅샷.
 *
 * <p>응답 DTO를 그대로 쓰지 않고 따로 둔 이유:
 * <ul>
 *   <li>응답에는 수정 일시, 비표준 검출 결과처럼 대상 자체의 변경이 아닌 값이 섞여 있어, 비교하면 늘 달라 보인다.</li>
 *   <li>응답 형식이 바뀌어도 과거 이력 JSON의 모양이 흔들리지 않게 하기 위해서다.</li>
 * </ul>
 * 스냅샷은 대상에 저장된 값과, 사람이 읽을 수 있도록 참조 대상의 ID·이름만 담는다.
 * sealed로 구현을 이 패키지의 record로 제한해, 이력 대상이 늘 때 한 곳에서 확인할 수 있게 한다.
 */
public sealed interface HistorySnapshot
        permits WordSnapshot, DomainSnapshot, TermSnapshot, TableSnapshot, ColumnSnapshot {

    /** 대상 ID. JSON에도 들어간다. */
    Long id();

    /** 대상 종류. 이력 행의 target_type이 되며, JSON에는 넣지 않는다. */
    @JsonIgnore
    HistoryTargetType targetType();
}
