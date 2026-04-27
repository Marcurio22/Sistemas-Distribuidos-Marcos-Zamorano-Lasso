package com.marcoszamorano.practica2.config;

import com.marcoszamorano.practica2.model.User;
import com.marcoszamorano.practica2.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedDefaultUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByUsername("marcos").isEmpty()) {
                User user = new User();
                user.setUsername("marcos");
                user.setPasswordHash(passwordEncoder.encode("marcos1234"));
                user.setEmail("marcos@practica.local");
                user.setRole("USER");
                user.setEnabled(true);
                userRepository.save(user);
            }
        };
    }
}