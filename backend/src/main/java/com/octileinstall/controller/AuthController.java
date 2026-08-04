package com.octileinstall.controller;

import com.octileinstall.dto.LoginRequest;
import com.octileinstall.dto.LoginResponse;
import com.octileinstall.security.UserPrincipal;
import com.octileinstall.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Exchanges credentials for a JWT. Intentionally the only unauthenticated endpoint —
     * there is no public registration on an admin tool; see DataSeeder for account creation.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Returns the caller's identity as resolved from their token. This is the end-to-end
     * proof that the filter chain populated the SecurityContext — if the filter silently
     * failed to run, this endpoint would 401 instead of returning a body.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of(
                "userId", principal.getId(),
                "email", principal.getEmail(),
                "fullName", principal.getFullName(),
                "role", principal.getRole().name()));
    }

    /**
     * Local handler so Phase 3 stands on its own; the Phase 6 @ControllerAdvice can absorb it later.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", HttpStatus.UNAUTHORIZED.value(),
                "error", "Unauthorized",
                "message", e.getMessage()));
    }
}
