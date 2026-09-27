package com.example.mk_backEnd.security;

import com.example.mk_backEnd.domain.User;
import com.example.mk_backEnd.repository.UserRepository;
import com.example.mk_backEnd.service.PlanService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PlanService planService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository, PlanService planService) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.planService = planService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parseClaims(token);
                String userId = claims.getSubject();
                String role = claims.get("role", String.class);

                User user = userRepository.findById(userId).orElse(null);

                // Removed from the workspace, or their seat is no longer covered by the plan (over
                // the people limit now that the workspace has fallen back to Free) - either way they
                // lose access immediately, not when the token happens to expire. Because this is
                // computed live off the current plan, paying again restores access with no separate
                // "reactivate" step: the very next request just starts passing.
                boolean blocked = user == null || user.getRemovedAt() != null || !planService.seatAllowed(user);
                if (blocked) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
