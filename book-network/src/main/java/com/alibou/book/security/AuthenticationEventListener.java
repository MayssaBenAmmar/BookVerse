package com.alibou.book.security;

import com.alibou.book.dashboard.DashboardService;
import com.alibou.book.dashboard.DashboardAuditLog;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationEventListener {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private UserRepository userRepository;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String email = authentication.getName();
                userRepository.findByEmail(email).ifPresent(user -> {
                    // Log login activity to dashboard
                    dashboardService.logActivity(
                            DashboardAuditLog.ActionType.LOGIN,
                            DashboardAuditLog.EntityType.AUTHENTICATION,
                            null,
                            null,
                            "User logged in successfully"
                    );
                    log.info("Logged authentication success for user: {}", user.getUsername());
                });
            }
        } catch (Exception e) {
            // Log error but don't throw exception to prevent login disruption
            log.error("Error logging authentication success: {}", e.getMessage(), e);
        }
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        try {
            Authentication authentication = event.getAuthentication();
            if (authentication != null) {
                String email = authentication.getName();
                userRepository.findByEmail(email).ifPresent(user -> {
                    // Log logout activity to dashboard
                    dashboardService.logActivity(
                            DashboardAuditLog.ActionType.LOGOUT,
                            DashboardAuditLog.EntityType.AUTHENTICATION,
                            null,
                            null,
                            "User logged out successfully"
                    );
                    log.info("Logged logout for user: {}", user.getUsername());
                });
            }
        } catch (Exception e) {
            log.error("Error logging logout: {}", e.getMessage(), e);
        }
    }
}