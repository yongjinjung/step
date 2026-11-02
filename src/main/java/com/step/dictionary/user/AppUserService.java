package com.step.dictionary.user;

import com.step.dictionary.global.error.BusinessException;
import com.step.dictionary.global.error.ErrorCode;
import com.step.dictionary.global.response.PageResponse;
import com.step.dictionary.role.Role;
import com.step.dictionary.role.RoleRepository;
import com.step.dictionary.role.RoleService;
import com.step.dictionary.user.dto.PasswordResetRequest;
import com.step.dictionary.user.dto.UserCreateRequest;
import com.step.dictionary.user.dto.UserResponse;
import com.step.dictionary.user.dto.UserSearchCondition;
import com.step.dictionary.user.dto.UserUpdateRequest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 계정 관리 업무 로직 (관리자 전용 API에서 쓴다). 접근 권한은 역할의 메뉴 권한이 막는다.
 *
 * <p>업무 규칙:
 * <ul>
 *   <li>아이디는 소문자로 정규화한 뒤 유일해야 한다 ({@code DUPLICATE_USERNAME}).</li>
 *   <li>계정은 삭제하지 않고 비활성화한다.</li>
 *   <li>사용자에게는 역할이 하나 이상 있어야 한다 ({@code USER_ROLE_REQUIRED}).</li>
 *   <li>전권 역할을 가진 활성 사용자가 한 명도 남지 않게 되는 수정(역할 제거, 비활성화)은 막는다
 *       ({@code LAST_ADMIN_REQUIRED}). 아무도 시스템을 관리할 수 없게 되는 상황을 막기 위해서다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class AppUserService {

    private final AppUserRepository userRepository;
    /** 부여할 역할 조회용 (없는 역할이면 404). */
    private final RoleService roleService;
    /** 마지막 전권 사용자 보호 검사용. */
    private final RoleRepository roleRepository;
    /** 비밀번호 해시 생성용 ({@code SecurityConfig}의 DelegatingPasswordEncoder, 기본 BCrypt). */
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository userRepository, RoleService roleService, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 계정을 만든다. 생성 직후 활성 상태다.
     *
     * @throws BusinessException USER_ROLE_REQUIRED (400), ROLE_NOT_FOUND (404), DUPLICATE_USERNAME (409)
     */
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        List<Role> roles = getRoles(request.roleIds());
        // 정규화한 값으로 검사해야 Hong과 hong을 같은 아이디로 판단한다.
        String username = AppUser.normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }

        AppUser user = userRepository.save(new AppUser(
                username, passwordEncoder.encode(request.password()), request.name(), roles));
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
     * <p>이미 발급된 JWT에는 이전 역할 코드가 들어 있지만, 기능 권한은 요청마다 DB에서 다시 계산하므로
     * 역할 변경은 바로 반영된다. 비활성화는 토큰이 만료돼야 완전히 막힌다(서버가 토큰 상태를 저장하지 않는 JWT 방식).
     *
     * @throws BusinessException USER_ROLE_REQUIRED (400), USER_NOT_FOUND, ROLE_NOT_FOUND (404),
     *                           LAST_ADMIN_REQUIRED (409)
     */
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = getUser(id);
        List<Role> roles = getRoles(request.roleIds());
        // 전권 활성 사용자였던 계정이 이 수정으로 그렇지 않게 되는 경우에만 남은 전권 사용자 수를 센다.
        // 동시에 두 관리자가 서로의 권한을 빼면 둘 다 통과할 수 있다. 관리 화면 사용 빈도상 허용한다.
        boolean staysSuperAdmin = request.enabled()
                && roles.stream().anyMatch(role -> role.isSuperAdmin() && role.isUsed());
        if (user.hasSuperAdminRole() && user.isEnabled() && !staysSuperAdmin
                && roleRepository.countOtherActiveSuperAdmins(id) == 0) {
            throw new BusinessException(ErrorCode.LAST_ADMIN_REQUIRED);
        }

        user.update(request.name(), roles, request.enabled());
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

    /** 역할 ID 목록을 실제 역할로 바꾼다. 비어 있으면 USER_ROLE_REQUIRED(400). */
    private List<Role> getRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BusinessException(ErrorCode.USER_ROLE_REQUIRED);
        }
        return roleService.getRoles(roleIds);
    }

    /** ID로 계정을 찾고, 없으면 USER_NOT_FOUND(404)를 던진다. */
    private AppUser getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
