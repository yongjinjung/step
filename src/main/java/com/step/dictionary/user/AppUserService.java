package com.step.dictionary.user;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.user.dto.PasswordResetRequest;
import com.step.dictionary.user.dto.UserCreateRequest;
import com.step.dictionary.user.dto.UserResponse;
import com.step.dictionary.user.dto.UserSearchCondition;
import com.step.dictionary.user.dto.UserUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정 관리 업무 로직 (관리자 전용 API에서 쓴다). 접근 권한은 {@code SecurityConfig}의 URL 규칙이 막는다.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>아이디는 소문자로 정규화한 뒤 유일해야 한다 ({@code DUPLICATE_USERNAME}).</li>
 *   <li>계정은 삭제하지 않고 비활성화한다.</li>
 *   <li>활성 관리자가 한 명도 남지 않게 되는 수정(역할 강등, 비활성화)은 막는다 ({@code LAST_ADMIN_REQUIRED}).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AppUserService {

    private final AppUserRepository userRepository;
    /** 비밀번호 해시 생성용 ({@code SecurityConfig}의 DelegatingPasswordEncoder, 기본 BCrypt). */
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 계정을 만든다. 생성 직후 활성 상태다.
     *
     * @throws BusinessException DUPLICATE_USERNAME (409)
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        // 정규화한 값으로 검사해야 Hong과 hong을 같은 아이디로 판단한다.
        String username = AppUser.normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        AppUser user = userRepository.save(new AppUser(
                username, passwordEncoder.encode(request.password()), request.name(), request.role()));
        return UserResponse.from(user);
    }

    /**
     * 계정을 단건 조회한다.
     *
     * @throws BusinessException USER_NOT_FOUND (404)
     */
    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    /** 조건으로 계정을 검색한다. 아이디 오름차순. */
    public PageResponse<UserResponse> search(UserSearchCondition condition, Pageable pageable) {
        return PageResponse.from(userRepository.search(condition, pageable).map(UserResponse::from));
    }

    /**
     * 이름, 역할, 활성 여부를 수정한다.
     *
     * <p>이미 발급된 JWT에는 이전 역할이 들어 있으므로, 역할 변경과 비활성화는 그 토큰이 만료된 뒤에 완전히 반영된다.
     * (서버가 토큰 상태를 저장하지 않는 JWT 방식의 한계. 만료 시간은 {@code app.jwt.expiration}.)
     *
     * @throws BusinessException USER_NOT_FOUND (404), LAST_ADMIN_REQUIRED (409)
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = getUser(id);
        // 활성 관리자였던 계정이 이 수정으로 활성 관리자가 아니게 되는 경우에만 남은 관리자 수를 센다.
        // 동시에 두 관리자가 서로를 강등하면 둘 다 통과할 수 있다. 관리 화면 사용 빈도상 허용한다.
        boolean staysActiveAdmin = request.role() == Role.ADMIN && request.enabled();
        if (user.isActiveAdmin() && !staysActiveAdmin
                && userRepository.countByRoleAndEnabled(Role.ADMIN, true) <= 1) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_REQUIRED);
        }

        user.update(request.name(), request.role(), request.enabled());
        // 즉시 update를 실행해 갱신된 updatedAt으로 응답한다.
        userRepository.flush();
        return UserResponse.from(user);
    }

    /**
     * 비밀번호를 초기화한다 (현재 비밀번호 확인 없음). 비밀번호를 잊은 사용자를 관리자가 도울 때 쓴다.
     *
     * @throws BusinessException USER_NOT_FOUND (404)
     */
    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        AppUser user = getUser(id);
        // 트랜잭션이 끝날 때 변경 감지로 update된다.
        user.changePassword(passwordEncoder.encode(request.password()));
    }

    /** ID로 계정을 찾고, 없으면 USER_NOT_FOUND(404)를 던진다. */
    private AppUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
