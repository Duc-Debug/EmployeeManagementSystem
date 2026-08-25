package com.hrm.employeemanagement.infrastructure.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.user.User;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.security.JwtTokenProviderAdapter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProviderAdapter tokenProvider;
    private final LoadUserPort loadUserPort;
    private final UserStatusCache userStatusCache;
    private final TokenBlacklistPort tokenBlacklistPort;

    public JwtAuthenticationFilter(JwtTokenProviderAdapter tokenProvider,
            LoadUserPort loadUserPort,
            UserStatusCache userStatusCache,
            TokenBlacklistPort tokenBlacklistPort) {
        this.tokenProvider = tokenProvider;
        this.loadUserPort = loadUserPort;
        this.userStatusCache = userStatusCache;
        this.tokenBlacklistPort = tokenBlacklistPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)
                    && (tokenBlacklistPort == null || !tokenBlacklistPort.isBlacklisted(jwt))
                    && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                long issuedAt = tokenProvider.getIssuedAtTimestampFromToken(jwt);

                if (tokenBlacklistPort != null && tokenBlacklistPort.isUserRevoked(username, issuedAt)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // High-performance Caffeine Cache lookup (Avoids DB hits on every request)
                Optional<User> userOpt = userStatusCache.get(username);
                if (userOpt.isEmpty()) {
                    userOpt = loadUserPort.findByUsername(username);
                    userOpt.filter(User::isActive).ifPresent(u -> userStatusCache.put(username, u));
                }

                // Verify user exists AND is ACTIVE
                if (userOpt.isPresent() && userOpt.get().isActive()) {
                    User user = userOpt.get();
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().getCode().getCode());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, Collections.singletonList(authority));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
