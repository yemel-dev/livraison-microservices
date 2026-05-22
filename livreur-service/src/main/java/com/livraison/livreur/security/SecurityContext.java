package com.livraison.livreur.security;

/**
 * Utilitaire Zero Trust : extrait les informations utilisateur
 * injectées par l'API Gateway dans les headers.
 */

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** en gros :
 * une classe utilitaire avec des méthodes statiques getCurrentUserId(),
 * getCurrentUserRole(), isAdmin(), isLivreur() qui lisent ces headers depuis la
 * requête en cours. Les services l'utilisent pour vérifier les droits.
 */
public class SecurityContext {

    public static Long getCurrentUserId() {
        String userId = getHeader(ZeroTrustFilter.HEADER_USER_ID);
        if (userId == null) return null;
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String getCurrentUserRole() {
        return getHeader(ZeroTrustFilter.HEADER_USER_ROLE);
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getCurrentUserRole());
    }

    public static boolean isLivreur() {
        return "LIVREUR".equalsIgnoreCase(getCurrentUserRole());
    }

    private static String getHeader(String headerName) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader(headerName);
        } catch (Exception e) {
            return null;
        }
    }
}
