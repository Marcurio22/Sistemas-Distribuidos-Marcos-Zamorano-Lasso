package com.marcoszamorano.practica2.service;

import com.marcoszamorano.practica2.dto.RegisterForm;
import com.marcoszamorano.practica2.exception.RegistrationValidationException;
import com.marcoszamorano.practica2.model.User;
import com.marcoszamorano.practica2.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService implements UserDetailsService {

    /**
     * Expresión regular sencilla para validar el formato del email
     * durante el proceso de registro.
     */
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Método requerido por Spring Security para cargar un usuario a partir
     * de su username durante el login.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities("ROLE_" + user.getRole())
                .disabled(!Boolean.TRUE.equals(user.getEnabled()))
                .build();
    }

    /**
     * Recupera la entidad completa del usuario desde base de datos.
     */
    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    /**
     * Registra un login correcto.
     *
     * Para ello:
     * - se genera un UUID
     * - se persiste en la tabla users
     * - se actualiza la fecha de último acceso
     * - se copia también la información mínima a la sesión HTTP
     */
    @Transactional
    public String registerSuccessfulLogin(String username, HttpSession session) {
        User user = getByUsername(username);

        String token = UUID.randomUUID().toString();
        user.setSessionToken(token);
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("sessionToken", token);

        return token;
    }

    /**
     * Limpia el token persistido cuando el usuario cierra sesión.
     * Así queda reflejado en base de datos que ya no existe una sesión activa.
     */
    @Transactional
    public void clearSessionToken(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setSessionToken(null);
            userRepository.save(user);
        });
    }

    /**
     * Registra un usuario nuevo en la aplicación.
     *
     * Antes de persistir se validan:
     * - nombre de usuario
     * - email
     * - contraseña
     * - confirmación de contraseña
     * - duplicados por username y email
     *
     * Si alguna validación falla, se lanza una excepción específica con
     * errores por campo para poder mostrarlos después en la vista.
     */
    @Transactional
    public void registerNewUser(RegisterForm form) {
        Map<String, String> errors = new LinkedHashMap<>();

        String username = safeTrim(form.getUsername());
        String email = safeTrim(form.getEmail()).toLowerCase();
        String password = form.getPassword() != null ? form.getPassword() : "";
        String confirmPassword = form.getConfirmPassword() != null ? form.getConfirmPassword() : "";

        if (username.isBlank()) {
            errors.put("username", "El nombre de usuario es obligatorio.");
        } else if (username.length() < 3 || username.length() > 30) {
            errors.put("username", "El nombre de usuario debe tener entre 3 y 30 caracteres.");
        } else if (userRepository.existsByUsername(username)) {
            errors.put("username", "Ese nombre de usuario ya está registrado.");
        }

        if (email.isBlank()) {
            errors.put("email", "El correo electrónico es obligatorio.");
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.put("email", "El formato del correo electrónico no es válido.");
        } else if (userRepository.existsByEmail(email)) {
            errors.put("email", "Ese correo electrónico ya está registrado.");
        }

        if (password.isBlank()) {
            errors.put("password", "La contraseña es obligatoria.");
        } else if (password.length() < 8) {
            errors.put("password", "La contraseña debe tener al menos 8 caracteres.");
        }

        if (confirmPassword.isBlank()) {
            errors.put("confirmPassword", "Debes confirmar la contraseña.");
        } else if (!password.equals(confirmPassword)) {
            errors.put("confirmPassword", "Las contraseñas no coinciden.");
        }

        if (!errors.isEmpty()) {
            throw new RegistrationValidationException(errors);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setEnabled(true);
        user.setSessionToken(null);
        user.setLastLoginAt(null);

        userRepository.save(user);
    }

    /**
     * Normaliza cadenas recibidas desde formularios evitando nulos
     * y recortando espacios laterales.
     */
    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}