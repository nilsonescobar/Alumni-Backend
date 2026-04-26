package org.fia.alumni.alumnifiauesbackend.security;

import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new BadRequestException("No autenticado");
        }
        return (Long) auth.getPrincipal();
    }

    public static String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) return null;
        return auth.getAuthorities().iterator().next().getAuthority();
    }

    public static String getCurrentUserType() {
        String role = getCurrentUserRole();
        if (role != null && role.startsWith("ROLE_")) {
            return role.substring(5);
        }
        return role;
    }
}