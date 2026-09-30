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
        // If it contains a wildcard '*', ensure it is restricted strictly to local dev or RFC 1918 private subnets
        if (trimmed.contains("*")) {
            boolean isLocalOrPrivateNet = trimmed.startsWith("http://localhost:")
                    || trimmed.startsWith("http://127.0.0.1:")
                    || trimmed.startsWith("http://192.168.")
                    || trimmed.startsWith("http://10.")
                    || (trimmed.startsWith("http://172.") && isRfc1918ClassB(trimmed));
            if (!isLocalOrPrivateNet) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRfc1918ClassB(String origin) {
        // RFC 1918: 172.16.0.0 - 172.31.255.255
        try {
            String prefix = origin.replace("http://172.", "");
            int dotIdx = prefix.indexOf('.');
            int colonIdx = prefix.indexOf(':');
            int endIdx = dotIdx != -1 ? dotIdx : (colonIdx != -1 ? colonIdx : prefix.length());
            String secondOctetStr = prefix.substring(0, endIdx).replace("*", "");
            if (secondOctetStr.isEmpty()) {
                return true; // 172.*
            }
            int secondOctet = Integer.parseInt(secondOctetStr);
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (Exception ignored) {
            return false;
        }
    }
}
