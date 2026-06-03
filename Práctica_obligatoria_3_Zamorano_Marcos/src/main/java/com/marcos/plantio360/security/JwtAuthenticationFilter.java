/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.security;

import com.marcos.plantio360.service.JwtService;
import com.marcos.plantio360.service.PlantioUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

/**
 * Filtro que reconstruye autenticación a partir del JWT.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String COOKIE_NAME = "PLANTIO360_JWT";
    private final JwtService jwtService;
    private final PlantioUserDetailsService userDetailsService;

    /**
     * Procesa una petición HTTP.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Optional<String> token = resolveToken(request);
        if (token.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String email = jwtService.extractSubject(token.get());
                if (email != null) {
                    var user = userDetailsService.loadUserByUsername(email);
                    if (jwtService.isTokenValid(token.get(), user.getUsername())) {
                        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    /** Resuelve token desde Bearer o cookie. */
    private Optional<String> resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) return Optional.of(header.substring(7));
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return Optional.empty();
        return Arrays.stream(cookies).filter(c -> COOKIE_NAME.equals(c.getName())).map(Cookie::getValue).findFirst();
    }
}
