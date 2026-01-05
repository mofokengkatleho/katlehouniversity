package com.katlehouniversity.ecd.service;

import com.katlehouniversity.ecd.dto.AuthDto;
import com.katlehouniversity.ecd.entity.User;
import com.katlehouniversity.ecd.repository.UserRepository;
import com.katlehouniversity.ecd.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(userDetails);

        log.info("Login successful for user: {}", request.getUsername());

        return AuthDto.LoginResponse.builder()
                .token(token)
                .type("Bearer")
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Transactional
    public User register(AuthDto.RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(User.Role.ADMIN)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());

        return user;
    }
}
