/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.config;

import com.marcos.plantio360.security.JwtAuthenticationFilter;
import com.marcos.plantio360.service.PlantioUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Seguridad stateless con JWT en cookie HttpOnly y cabecera Bearer.
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PlantioUserDetailsService userDetailsService;

    /** Configura la cadena de seguridad HTTP. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Autenticación requerida");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/login?required=true");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado")))
            .logout(logout -> logout
                .logoutUrl("/logout")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies(JwtAuthenticationFilter.COOKIE_NAME)
                .logoutSuccessHandler((request, response, authentication) -> {
                    SecurityContextHolder.clearContext();
                    ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
                    response.sendRedirect(request.getContextPath() + "/?logout=true");
                }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/logout", "/css/**", "/js/**", "/images/**", "/actuator/health", "/error").permitAll()
                .requestMatchers("/players/**", "/matches/**", "/shop/**", "/map", "/api/players", "/api/matches", "/api/products", "/api/map-points", "/api/sensors/latest").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        return http.build();
    }

    /** Codificador BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    /** Proveedor DAO. */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** AuthenticationManager global. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
