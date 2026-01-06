package com.seathold.api.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.seathold.api.domain.user.UserRole;
import com.seathold.api.domain.user.User;
import com.seathold.api.domain.user.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByEmail("admin@seathold.com")) {
                User admin = User.builder()
                        .email("admin@seathold.com")
                        .password(passwordEncoder.encode("admin123"))
                        .firstName("Ryan")
                        .lastName("Felix")
                        .role(UserRole.ADMIN)
                        .build();

                userRepository.save(admin);
            }
        };
    }
}