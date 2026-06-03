/**
 * El Plantío 360 - Plataforma inteligente para aficionados.
 *
 * Autor: Marcos Zamorano Lasso
 * Práctica 3 - Sistemas Distribuidos
 */

package com.marcos.plantio360.service;

import com.marcos.plantio360.dto.RegisterRequest;
import com.marcos.plantio360.model.AppUser;
import com.marcos.plantio360.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Servicio de registro, búsqueda y mantenimiento de usuarios.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registra un usuario final.
     *
     * @param request datos recibidos.
     * @return usuario creado.
     */
    @Transactional
    public AppUser register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) throw new IllegalArgumentException("Email ya registrado");
        AppUser user = AppUser.builder()
            .firstName(request.getFirstName()).lastName(request.getLastName()).email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword())).phone(request.getPhone())
            .role("ROLE_USER").enabled(true).avatarUrl("/images/avatar.svg").build();
        return userRepository.save(user);
    }

    /** Devuelve todos los usuarios. */
    public List<AppUser> findAll() { return userRepository.findAll(); }

    /** Busca por id. */
    public AppUser findById(Long id) { return userRepository.findById(id).orElseThrow(); }

    /** Busca por email. */
    public AppUser findByEmail(String email) { return userRepository.findByEmail(email).orElseThrow(); }

    /**
     * Guarda usuario desde administración, codificando contraseña si procede.
     *
     * @param user usuario recibido.
     * @return usuario guardado.
     */
    @Transactional
    public AppUser saveAdmin(AppUser user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            user.setPassword(findById(user.getId()).getPassword());
        } else if (!user.getPassword().startsWith("$2")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    /** Elimina usuario por id. */
    public void delete(Long id) { userRepository.deleteById(id); }
}
