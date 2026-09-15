package com.step.dictionary.history;

import com.step.dictionary.history.snapshot.HistorySnapshot;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 변경 이력 기록기. 대상 데이터를 바꾸는 서비스가 변경 직후 호출한다.
 *
 * <p>호출한 서비스의 트랜잭션에 참여해 저장하므로, 변경이 롤백되면 이력도 함께 롤백된다.
 * 변경자는 요청 스레드의 인증 정보(JWT subject)에서, 신청 ID는 {@link HistoryRequestContext}에서 가져온다.
 * 그래서 서비스는 스냅샷만 넘기면 된다.
 *
 * <pre>
 * WordSnapshot before = WordSnapshot.from(word);   // 변경 전에 떠 둔다
 * word.update(...);
 * historyRecorder.recordUpdate(before, WordSnapshot.from(word));
 * </pre>
 */
@Component
public class ChangeHistoryRecorder {

    private final ChangeHistoryRepository historyRepository;
    /** 변경자 조회용. */
    private final AppUserRepository userRepository;
    /** 스냅샷 JSON 직렬화 (스프링 부트의 Jackson 3 매퍼). */
    private final JsonMapper jsonMapper;
    /** 변경 일시 계산용. */
    private final Clock clock;

    public ChangeHistoryRecorder(ChangeHistoryRepository historyRepository, AppUserRepository userRepository,
            JsonMapper jsonMapper, Clock clock) {
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.jsonMapper = jsonMapper;
        this.clock = clock;
    }

    /** 등록 이력. 저장 후(ID가 생긴 뒤)의 스냅샷을 넘긴다. */
    public void recordCreate(HistorySnapshot after) {
        save(after, ChangeType.CREATE, null, write(after));
    }

    /**
     * 수정 이력. 전후 스냅샷이 같으면(값이 하나도 바뀌지 않은 PUT) 남기지 않는다.
     * 스냅샷에는 수정 일시 같은 자동 값이 없어 실제 데이터 변경만 비교된다.
     */
    public void recordUpdate(HistorySnapshot before, HistorySnapshot after) {
        String beforeData = write(before);
        String afterData = write(after);
        if (beforeData.equals(afterData)) {
            return;
        }
        save(after, ChangeType.UPDATE, beforeData, afterData);
    }

    /** 삭제 이력. 삭제하기 전의 스냅샷을 넘긴다. */
    public void recordDelete(HistorySnapshot before) {
        save(before, ChangeType.DELETE, write(before), null);
    }

    private void save(HistorySnapshot snapshot, ChangeType changeType, String beforeData, String afterData) {
        historyRepository.save(new ChangeHistory(
                snapshot.targetType(), snapshot.id(), changeType, beforeData, afterData,
                currentUser(), HistoryRequestContext.currentRequestId(), LocalDateTime.now(clock)));
    }

    /** 스냅샷 record를 JSON으로. record 구성 요소 순서대로 써지므로 같은 값이면 같은 문자열이 나온다. */
    private String write(HistorySnapshot snapshot) {
        return jsonMapper.writeValueAsString(snapshot);
    }

    /**
     * 현재 요청의 로그인 사용자. 인증 정보가 없거나 익명이면 null(시스템 작업)이다.
     * 인증 이름은 JWT subject(소문자 아이디)다.
     */
    private AppUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
