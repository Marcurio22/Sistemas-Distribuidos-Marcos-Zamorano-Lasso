package com.marcoszamorano.practica2.config;

import com.marcoszamorano.practica2.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
public class SecurityConfig {

    /**
     * Codificador de contraseñas usado en todo el sistema.
     * Se utiliza BCrypt por ser una opción estándar y segura para almacenar
     * contraseñas cifradas en base de datos.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Proveedor de autenticación basado en base de datos.
     * Spring Security delega en UserService la carga del usuario y utiliza
     * el PasswordEncoder anterior para comparar la contraseña introducida.
     */
    @Bean
    public AuthenticationProvider authenticationProvider(UserService userService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    /**
     * Handler ejecutado cuando el login se completa correctamente.
     * Aquí se aprovecha para registrar el acceso en base de datos:
     * - se genera el token UUID de sesión
     * - se actualiza la fecha del último login
     * - se guarda la información mínima en la sesión HTTP
     *
     * Después de esto se redirige al laboratorio, que es la pantalla central
     * de demostración de la práctica.
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler(UserService userService) {
        return (request, response, authentication) -> {
            userService.registerSuccessfulLogin(authentication.getName(), request.getSession(true));
            response.sendRedirect("/lab");
        };
    }

    /**
     * Handler ejecutado al cerrar sesión.
     * Si el usuario estaba autenticado, se limpia el token persistido en base
     * de datos y se redirige al login indicando que el logout ha sido correcto.
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler(UserService userService) {
        return (request, response, authentication) -> {
            if (authentication != null) {
                userService.clearSessionToken(authentication.getName());
            }
            response.sendRedirect("/login?logout=true");
        };
    }

    /**
     * Configuración principal de seguridad de Spring.
     *
     * Decisiones adoptadas:
     * - Home, login, registro y recursos estáticos son públicos
     * - El resto de rutas requieren autenticación
     * - Se usa formulario de login personalizado
     * - El logout invalida la sesión y elimina la cookie JSESSIONID
     * - Los accesos denegados redirigen a una vista 403 propia
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationProvider authenticationProvider,
                                           AuthenticationSuccessHandler authenticationSuccessHandler,
                                           LogoutSuccessHandler logoutSuccessHandler) throws Exception {

        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/register", "/error", "/css/**", "/js/**", "/images/**", "/forbidden").permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/forbidden")
                );

        return http.build();
    }
}