package com.hrm.employeemanagement.infrastructure.security;

import com.hrm.employeemanagement.application.port.outbound.security.TokenBlacklistPort;
import com.hrm.employeemanagement.application.port.outbound.security.TokenProviderPort;
import com.hrm.employeemanagement.application.port.outbound.user.LoadUserPort;
import com.hrm.employeemanagement.domain.user.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.hrm.employeemanagement.infrastructure.adapter.outbound.persistence.authorization.SpringDataPermissionRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProviderPort tokenProvider;
    private final LoadUserPort loadUserPort;
    private final UserStatusCache userStatusCache;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final SpringDataPermissionRepository permissionRepository;

    @Autowired
    public JwtAuthenticationFilter(TokenProviderPort tokenProvider,
            LoadUserPort loadUserPort,
            UserStatusCache userStatusCache,
            TokenBlacklistPort tokenBlacklistPort,
            @Autowired(required = false) SpringDataPermissionRepository permissionRepository) {
        this.tokenProvider = java.util.Objects.requireNonNull(tokenProvider, "tokenProvider must not be null");
        this.loadUserPort = java.util.Objects.requireNonNull(loadUserPort, "loadUserPort must not be null");
        this.userStatusCache = java.util.Objects.requireNonNull(userStatusCache, "userStatusCache must not be null");
        this.tokenBlacklistPort = java.util.Objects.requireNonNull(tokenBlacklistPort, "tokenBlacklistPort must not be null");
        this.permissionRepository = permissionRepository;
    }

    public JwtAuthenticationFilter(TokenProviderPort tokenProvider,
            LoadUserPort loadUserPort,
            UserStatusCache userStatusCache,
            TokenBlacklistPort tokenBlacklistPort) {
        this(tokenProvider, loadUserPort, userStatusCache, tokenBlacklistPort, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)
                    && !tokenBlacklistPort.isBlacklisted(jwt)
                    && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                long issuedAt = tokenProvider.getIssuedAtTimestampFromToken(jwt);

                if (tokenBlacklistPort.isUserRevoked(username, issuedAt)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // High-performance Caffeine Cache lookup
                Optional<User> userOpt = userStatusCache.get(username);
                if (userOpt.isEmpty()) {
                    userOpt = loadUserPort.findByUsername(username);
                    userOpt.filter(User::isActive).ifPresent(u -> userStatusCache.put(username, u));
                }

                if (userOpt.isPresent() && userOpt.get().isActive()) {
                    User user = userOpt.get();

                    // Session Invalidation Check: Deterministic tokenVersion check
                    Integer jwtTokenVersion = tokenProvider.getTokenVersionFromToken(jwt);
                    if (jwtTokenVersion != null) {
                        if (!jwtTokenVersion.equals(user.getTokenVersion())) {
                            userStatusCache.evict(username);
                            filterChain.doFilter(request, response);
                            return;
                        }
                    } else {
                        Date tokenIssuedAt = tokenProvider.getIssuedAtFromToken(jwt);
                        if (user.getPasswordChangedAt() != null && tokenIssuedAt != null) {
                            long passwordChangedEpochSec = user.getPasswordChangedAt().getEpochSecond();
                            long tokenIssuedEpochSec = tokenIssuedAt.getTime() / 1000;
                            if (tokenIssuedEpochSec < passwordChangedEpochSec) {
                                userStatusCache.evict(username);
                                filterChain.doFilter(request, response);
                                return;
                            }
                        }
                    }

                    List<GrantedAuthority> authorities = new ArrayList<>();
                    String roleCode = user.getRole().getCode().getCode();
                    authorities.add(new SimpleGrantedAuthority(roleCode));
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));

                    if (permissionRepository != null && user.getIdValue() != null) {
                        List<String> permCodes = permissionRepository.findPermissionCodesByUserId(user.getIdValue());
                        if (permCodes != null) {
                            for (String perm : permCodes) {
                                authorities.add(new SimpleGrantedAuthority(perm));
                            }
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            user, null, authorities);
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
