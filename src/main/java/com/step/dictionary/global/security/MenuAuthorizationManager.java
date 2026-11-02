package com.step.dictionary.global.security;

import com.step.dictionary.global.security.ApiPermissionRules.Requirement;
import com.step.dictionary.role.PermissionService;
import com.step.dictionary.role.UserPermissions;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

/**
 * 역할의 메뉴 권한으로 요청을 허용할지 정한다 ({@code SecurityConfig}의 {@code anyRequest().access(...)}).
 *
 * <p>순서:
 * <ol>
 *   <li>로그인하지 않았으면 거부한다 (인증 오류로 401).</li>
 *   <li>{@link ApiPermissionRules}로 요청에 필요한 권한(메뉴 + 동작)을 찾는다.
 *       규칙이 없는 경로는 거부한다. 새 API를 만들고 규칙을 빠뜨리면 막히도록 하기 위해서다.</li>
 *   <li>사용자의 실제 권한을 계산해(역할들의 합집합) 필요한 권한 중 하나라도 만족하면 허용한다.
 *       전권 역할이 있으면 무조건 허용한다.</li>
 * </ol>
 *
 * <p>거부되면 {@code SecurityErrorHandler}가 403(FORBIDDEN) JSON을 돌려준다.
 */
@Component
public class MenuAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final PermissionService permissionService;

    public MenuAuthorizationManager(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
            RequestAuthorizationContext context) {
        Authentication auth = authentication.get();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }

        HttpServletRequest request = context.getRequest();
        List<Requirement> required = ApiPermissionRules.requirementsFor(request.getMethod(), request.getRequestURI());
        if (required.isEmpty()) {
            return new AuthorizationDecision(false);
        }

        UserPermissions permissions = permissionService.of(auth.getName());
        boolean allowed = required.stream()
                .anyMatch(requirement -> permissions.can(requirement.menuPath(), requirement.action()));
        return new AuthorizationDecision(allowed);
    }
}
