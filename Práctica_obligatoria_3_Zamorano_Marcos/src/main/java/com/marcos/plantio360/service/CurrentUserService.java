/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;

/**
 * Servicio para resolver el usuario autenticado por JWT.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserService userService;

    /** Devuelve usuario actual si existe. */
    public Optional<AppUser> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return Optional.empty();
        return Optional.of(userService.findByEmail(auth.getName()));
    }

    /** Devuelve usuario actual o lanza error. */
    public AppUser require() { return current().orElseThrow(() -> new IllegalStateException("Usuario no autenticado")); }
}
