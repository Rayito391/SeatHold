package com.seathold.api.security;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.seathold.api.common.exception.SecurityExcepction;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleValidator {
    private final JwtService jwtService;

    public void requireAdminRole(HttpServletRequest request) {
        UserInfo userInfo = extractUserInfo(request);

        if (!"ADMIN".equals(userInfo.role())) {
            log.warn("Access denied. Require ADMIN role : {}", userInfo.role());
            throw new SecurityExcepction("Access denied. admin role require");
        }
        log.debug("Admin access granted for user {}", userInfo.userId());
    }

    public void requireUser(HttpServletRequest request) {
        UserInfo userInfo = extractUserInfo(request);

        if (userInfo.role() == null ||
                (!userInfo.role().equals("USER") && !userInfo.role().equals("ADMIN"))) {
            log.warn("Access denied. Required: USER or ADMIN, Found: {} for user: {}",
                    userInfo.role(), userInfo.userId());
            throw new SecurityExcepction("Access denied. Valid user role required.");
        }

        log.debug("User access granted for user: {} with role: {}",
                userInfo.userId(), userInfo.role());
    }

    public UserInfo extractUserInfo(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("No Authorization header found");
                throw new SecurityExcepction("Authorization header missing");
            }

            String token = authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {
                log.warn("Invalid token provided");
                throw new SecurityExcepction("Invalid token");
            }

            UUID userId = jwtService.extractUserId(token);
            String email = jwtService.extractEmail(token);
            String role = jwtService.extractUserRole(token);

            return new UserInfo(userId, email, role);

        } catch (SecurityExcepction e) {
            throw e;
        } catch (Exception e) {
            log.error("Error extracting user info from token", e);
            throw new SecurityExcepction("Invalid token format");
        }
    }

    public record UserInfo(UUID userId, String email, String role) {
    }

}
