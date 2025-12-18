package com.example.JobFinder.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(authentication);
        log.info("User {} authenticated successfully, redirecting to {}", authentication.getName(), targetUrl);
        response.sendRedirect(targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN")) {
                return "/admin/dashboard";
            } else if (role.equals("ROLE_EMPLOYER")) {
                return "/employer/dashboard";
            } else if (role.equals("ROLE_CANDIDATE")) {
                return "/candidate/dashboard";
            }
        }
        // Default fallback
        return "/dashboard";
    }
}
