package com.seathold.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.seathold.api.auth.dto.LoginRequest;
import com.seathold.api.auth.dto.LoginResponse;
import com.seathold.api.auth.dto.RegisterRequest;
import com.seathold.api.auth.dto.RegisterResponse;
import com.seathold.api.common.exception.ConflictException;
import com.seathold.api.common.exception.SecurityExcepction;
import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;
import com.seathold.api.domain.user.UserRole;
import com.seathold.api.domain.user.User;
import com.seathold.api.domain.user.UserRepository;
import com.seathold.api.security.JwtService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for email: {}", request.email());

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SecurityExcepction("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.email());
            throw new SecurityExcepction("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getUuid(), user.getEmail(), user.getRole().name());

        LoginResponse response = LoginResponse.builder()
                .token(token)
                .userId(user.getUuid())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .role(user.getRole().name())
                .build();

        log.info("Login successful for user: {}", request.email());
        return ApiResponseFactory.successResponse(response, HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .build();

        User saved = userRepository.save(user);

        RegisterResponse response = RegisterResponse.builder()
                .userId(saved.getUuid())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .role(saved.getRole().name())
                .message("User registered successfully")
                .build();

        log.info("User registered successfully: {}", request.email());
        return ApiResponseFactory.successResponse(response);
    }

    @PostMapping("/create-admin")
    public ResponseEntity<ApiResponse<RegisterResponse>> createAdmin(@Valid @RequestBody RegisterRequest request) {
        log.info("Creating admin user: {}", request.email());

        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }

        User admin = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .build();

        User saved = userRepository.save(admin);

        RegisterResponse response = RegisterResponse.builder()
                .userId(saved.getUuid())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .role(saved.getRole().name())
                .message("Admin user created successfully")
                .build();

        log.info("Admin user created successfully: {}", request.email());
        return ApiResponseFactory.successResponse(response);
    }
}
