package com.example.task_flow_backend.auth;

import com.example.task_flow_backend.auth.dto.AuthResponse;
import com.example.task_flow_backend.auth.dto.LoginRequest;
import com.example.task_flow_backend.auth.dto.RefreshRequest;
import com.example.task_flow_backend.auth.dto.RegisterRequest;
import com.example.task_flow_backend.common.exception.EmailAlreadyUsedException;
import com.example.task_flow_backend.common.exception.InvalidCredentialsException;
import com.example.task_flow_backend.user.Role;
import com.example.task_flow_backend.user.User;
import com.example.task_flow_backend.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException("An account with this email already exists");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(Role.MEMBER);
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }
        if (!TokenType.REFRESH.name().equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
            throw new InvalidCredentialsException("Provided token is not a refresh token");
        }
        User user = userRepository.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new InvalidCredentialsException("Account no longer exists"));
        if (!user.isActive()) {
            throw new InvalidCredentialsException("Account is disabled");
        }
        return new AuthResponse(
                jwtService.generateAccessToken(user),
                request.refreshToken(),
                "Bearer",
                jwtProperties.accessTokenTtl().toSeconds());
    }

    private AuthResponse issueTokens(User user) {
        return AuthResponse.bearer(
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                jwtProperties.accessTokenTtl().toSeconds());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
