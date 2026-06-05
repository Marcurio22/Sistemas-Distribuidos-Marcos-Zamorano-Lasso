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
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()
            && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Las contraseñas no coinciden");
        }
        AppUser user = AppUser.builder()
            .firstName(request.getFirstName()).lastName(request.getLastName()).email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword())).phone(normalizePhone(request.getPhone()))
            .role("ROLE_USER").enabled(true).avatarUrl("/images/avatar.svg").build();
        return userRepository.save(user);
    }

    /**
     * Normaliza el teléfono al formato internacional visible del prototipo.
     *
     * @param phone teléfono introducido por el usuario.
     * @return teléfono con prefijo y agrupación legible, o null si no se indicó.
     */
    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String trimmed = phone.trim().replaceAll("\\s+", " ");
        String compact = trimmed.replaceAll("[^+0-9]", "");
        if (compact.startsWith("00")) compact = "+" + compact.substring(2);
        if (!compact.startsWith("+")) {
            String digits = compact.replaceAll("\\D", "");
            if (digits.startsWith("34") && digits.length() == 11) digits = digits.substring(2);
            compact = "+34" + digits;
        }
        String digits = compact.substring(1).replaceAll("\\D", "");
        if (digits.length() <= 9) return "+34 " + groupLocalNumber(digits);
        String prefix = digits.substring(0, digits.length() - 9);
        String local = digits.substring(digits.length() - 9);
        return "+" + prefix + " " + groupLocalNumber(local);
    }

    /** Agrupa un número local como 999 99 99 99 cuando tiene nueve cifras. */
    private String groupLocalNumber(String local) {
        if (local == null || local.length() != 9) return local;
        return local.substring(0, 3) + " " + local.substring(3, 5) + " " + local.substring(5, 7) + " " + local.substring(7, 9);
    }

    /** Devuelve todos los usuarios. */
    public List<AppUser> findAll() { return userRepository.findAll(); }

    /** Busca por id. */
    public AppUser findById(Long id) { return userRepository.findById(id).orElseThrow(); }

    /** Busca por email. */
    public AppUser findByEmail(String email) { return userRepository.findByEmail(email).orElseThrow(); }

    /**
     * Guarda usuario desde administración sin permitir cambios de contraseña desde el panel.
     *
     * @param user usuario recibido.
     * @return usuario guardado.
     */
    @Transactional
    public AppUser saveAdmin(AppUser user) {
        validateAdminUser(user);
        AppUser persisted = user.getId() == null ? null : findById(user.getId());
        validateUniqueEmail(user, persisted);

        if (persisted != null) {
            user.setPassword(persisted.getPassword());
            user.setCreatedAt(persisted.getCreatedAt());
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(persisted.getAvatarUrl());
            }
        } else {
            user.setPassword(passwordEncoder.encode("user1234"));
        }
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl("/images/avatar.svg");
        }
        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("ROLE_USER");
        }
        return userRepository.save(user);
    }

    /**
     * Valida campos básicos editables desde administración.
     *
     * @param user usuario recibido.
     */
    private void validateAdminUser(AppUser user) {
        if (user.getFirstName() == null || user.getFirstName().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio.");
        if (user.getLastName() == null || user.getLastName().isBlank()) throw new IllegalArgumentException("Los apellidos son obligatorios.");
        if (user.getEmail() == null || user.getEmail().isBlank()) throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        if (!user.getEmail().contains("@")) throw new IllegalArgumentException("El correo electrónico no tiene un formato válido.");
        if (user.getRole() == null || !(user.getRole().equals("ROLE_USER") || user.getRole().equals("ROLE_ADMIN"))) {
            throw new IllegalArgumentException("El rol seleccionado no es válido.");
        }
    }

    /**
     * Evita duplicados de email al crear o editar usuarios.
     *
     * @param user usuario recibido.
     * @param persisted usuario persistido, si se está editando.
     */
    private void validateUniqueEmail(AppUser user, AppUser persisted) {
        userRepository.findByEmail(user.getEmail()).ifPresent(existing -> {
            if (persisted == null || !existing.getId().equals(persisted.getId())) {
                throw new IllegalArgumentException("Ya existe un usuario con ese correo electrónico.");
            }
        });
    }

    /** Elimina usuario por id. */
    public void delete(Long id) { userRepository.deleteById(id); }
}
