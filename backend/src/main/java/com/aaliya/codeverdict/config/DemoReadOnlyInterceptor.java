package com.aaliya.codeverdict.config;

import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** On a public deployment the demo account may look around but not change anything or spend AI quota. */
@Component
public class DemoReadOnlyInterceptor implements HandlerInterceptor {

    private final CodeVerdictProperties props;

    public DemoReadOnlyInterceptor(CodeVerdictProperties props) {
        this.props = props;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!props.demo().readOnly() || "GET".equals(request.getMethod()) || "OPTIONS".equals(request.getMethod())
                || request.getRequestURI().startsWith("/api/v1/auth/")) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthUser user && user.isDemo()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "DEMO_READ_ONLY",
                    "The demo account is read-only. Run CodeVerdict locally to submit your own code.");
        }
        return true;
    }
}
