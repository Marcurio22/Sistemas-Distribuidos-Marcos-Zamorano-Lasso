/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * Adaptador JPA para Spring Security.
 */
@Service
@RequiredArgsConstructor
public class PlantioUserDetailsService implements UserDetailsService {
    private final AppUserRepository userRepository;

    /**
     * Carga usuario por email.
     *
     * @param username email.
     * @return UserDetails.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = userRepository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(user.getEmail()).password(user.getPassword()).disabled(!user.isEnabled()).authorities(user.getRole()).build();
    }
}
