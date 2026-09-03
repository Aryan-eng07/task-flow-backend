package com.example.task_flow_backend.auth;

import com.example.task_flow_backend.auth.dto.AuthResponse;
import com.example.task_flow_backend.auth.dto.LoginRequest;
import com.example.task_flow_backend.auth.dto.RefreshRequest;
import com.example.task_flow_backend.auth.dto.RegisterRequest;
import com.example.task_flow_backend.auth.dto.UserProfileResponse;
import com.example.task_flow_backend.common.exception.InvalidCredentialsException;
import com.example.task_flow_backend.user.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new InvalidCredentialsException("Not authenticated");
        }
        return userRepository.findById(principal.id())
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
