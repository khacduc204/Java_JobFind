package com.example.JobFinder.controller;

import com.example.JobFinder.dto.RegistrationRequest;
import com.example.JobFinder.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Development/Debug controller for testing authentication.
 * REMOVE THIS IN PRODUCTION!
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugAuthController {

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    @GetMapping("/hash-password")
    public ResponseEntity<Map<String, String>> hashPassword(@RequestParam String password) {
        String hashed = passwordEncoder.encode(password);
        log.info("Generated hash for password '{}': {}", password, hashed);
        return ResponseEntity.ok(Map.of(
            "password", password,
            "hash", hashed,
            "length", String.valueOf(hashed.length())
        ));
    }

    @GetMapping("/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(
            @RequestParam String raw,
            @RequestParam String hash) {
        boolean matches = passwordEncoder.matches(raw, hash);
        log.info("Testing password '{}' against hash '{}': {}", raw, hash, matches);
        return ResponseEntity.ok(Map.of(
            "raw", raw,
            "hash", hash,
            "matches", matches
        ));
    }

    @PostMapping("/create-test-user")
    public ResponseEntity<Map<String, String>> createTestUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(defaultValue = "3") Integer roleId) {
        try {
            RegistrationRequest request = new RegistrationRequest(email, password, "Test User", roleId);
            var user = userService.registerUser(request);
            log.info("Created test user: {}", user.getEmail());
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "email", user.getEmail(),
                "message", "User created successfully. You can now login with this email and password."
            ));
        } catch (Exception e) {
            log.error("Failed to create test user", e);
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
