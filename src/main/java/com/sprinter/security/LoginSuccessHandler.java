package com.sprinter.security;

import com.sprinter.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Zachycuje úspěšné přihlášení a aktualizuje časová razítka přihlášení uživatele.
 * Ukládá čas předchozího přihlášení (pro activity feed na dashboardu).
 */
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof SprinterUserDetails ud) {
            userService.recordLogin(ud.getUserId());
        }

        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(false);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
