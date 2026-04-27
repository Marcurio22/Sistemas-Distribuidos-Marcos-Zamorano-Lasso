package com.marcoszamorano.practica2.service;

import com.marcoszamorano.practica2.model.User;
import com.marcoszamorano.practica2.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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

    @Transactional(readOnly = true)
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

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

    @Transactional
    public void clearSessionToken(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setSessionToken(null);
            userRepository.save(user);
        });
    }
}