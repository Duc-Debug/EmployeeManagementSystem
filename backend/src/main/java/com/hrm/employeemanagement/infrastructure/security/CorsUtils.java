package com.hrm.employeemanagement.infrastructure.security;

/**
 * Utility to validate CORS origin patterns and prevent dangerous wildcard injection.
 */
public final class CorsUtils {

    private CorsUtils() {
        // Prevent instantiation
    }

    /**
     * Checks whether an origin pattern is safe to configure for credentialed CORS and CSRF filter.
     * Rejects global wildcards ('*') and wildcard subdomains on shared hosting domains (e.g. *.vercel.app, *.onrender.com).
     *
     * @param raw the origin pattern string
     * @return true if safe; false otherwise
     */
    public static boolean isSafeOriginPattern(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String trimmed = raw.trim();
        if ("*".equals(trimmed)) {
            return false;
        }
        // Disallow wildcard subdomains on shared public cloud/hosting platforms
        if (trimmed.contains("*.vercel.app")
                || trimmed.contains("*.onrender.com")
                || trimmed.contains("*.netlify.app")
                || trimmed.contains("*.github.io")
                || trimmed.contains("*.herokuapp.com")
                || trimmed.contains("*.pages.dev")
                || trimmed.contains("*.azurewebsites.net")) {
            return false;
        }
        // If it contains a wildcard '*', ensure it is restricted strictly to local dev or private subnets
        if (trimmed.contains("*")) {
            boolean isLocalOrPrivateNet = trimmed.startsWith("http://localhost:")
                    || trimmed.startsWith("http://127.0.0.1:")
                    || trimmed.startsWith("http://192.168.")
                    || trimmed.startsWith("http://10.")
                    || trimmed.startsWith("http://172.16.")
                    || trimmed.startsWith("http://26.");
            if (!isLocalOrPrivateNet) {
                return false;
            }
        }
        return true;
    }
}
