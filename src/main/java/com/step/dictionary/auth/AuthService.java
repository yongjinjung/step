package com.step.dictionary.auth;

import com.step.dictionary.auth.dto.LoginRequest;
import com.step.dictionary.auth.dto.PasswordChangeRequest;
import com.step.dictionary.auth.dto.TokenResponse;
import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.user.AppUser;
import com.step.dictionary.user.AppUserRepository;
import com.step.dictionary.user.dto.UserResponse;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인과 본인 계정 기능.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>아이디가 없거나 비밀번호가 틀리면 같은 오류({@code INVALID_CREDENTIALS}, 401)를 준다.
 *       어느 쪽이 틀렸는지 알려주면 존재하는 아이디를 알아낼 수 있기 때문이다.</li>
 *   <li>비밀번호가 맞아도 비활성 계정이면 로그인할 수 없다 ({@code USER_DISABLED}, 403).</li>
 *   <li>내 비밀번호 변경은 현재 비밀번호를 확인한다 ({@code INVALID_CURRENT_PASSWORD}, 400).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    /**
     * 없는 아이디로 로그인할 때 비교에 쓰는 가짜 해시.
     * 아이디가 없을 때 바로 응답하면 BCrypt 비교를 하는 경우보다 훨씬 빨라, 응답 시간으로 아이디 존재 여부가 드러난다.
     * 그래서 없는 아이디도 같은 비용의 비교를 한 번 한다.
     */
    private final String dummyPasswordHash;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /**
     * 아이디/비밀번호를 확인하고 액세스 토큰을 발급한다.
     *
     * @throws BusinessException INVALID_CREDENTIALS (401), USER_DISABLED (403)
     */
    public TokenResponse login(LoginRequest request) {
        Optional<AppUser> found = userRepository.findByUsername(AppUser.normalizeUsername(request.username()));
        if (found.isEmpty()) {
            // 결과는 버린다. 응답 시간을 맞추기 위한 비교다.
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        AppUser user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        // 비밀번호가 맞은 뒤에만 비활성 여부를 알려준다. 비밀번호를 모르는 사람에게는 상태를 드러내지 않는다.
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        return tokenProvider.issue(user);
    }

    /**
     * 로그인한 사용자 본인의 정보를 조회한다.
     *
     * @param username 토큰의 subject (소문자 아이디)
     * @throws BusinessException USER_NOT_FOUND (404). 토큰은 유효하지만 계정 행이 없어진 드문 경우
     */
    public UserResponse me(String username) {
        return UserResponse.from(getUser(username));
    }

    /**
     * 본인 비밀번호를 바꾼다. 이미 발급된 토큰은 만료 전까지 계속 쓸 수 있다.
     *
     * @param username 토큰의 subject
     * @throws BusinessException INVALID_CURRENT_PASSWORD (400), USER_NOT_FOUND (404)
     */
    @Transactional
    public void changePassword(String username, PasswordChangeRequest request) {
        AppUser user = getUser(username);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        // 트랜잭션이 끝날 때 변경 감지로 update된다.
        user.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    /** 아이디로 계정을 찾고, 없으면 USER_NOT_FOUND(404)를 던진다. */
    private AppUser getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
