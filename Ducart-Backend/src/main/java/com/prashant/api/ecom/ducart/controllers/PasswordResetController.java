package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.modal.*;
import com.prashant.api.ecom.ducart.services.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class PasswordResetController {
    private final PasswordResetService service;
    public PasswordResetController(PasswordResetService service) { this.service = service; }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody PasswordResetRequest body,
            HttpServletRequest request) {
        service.request(body.usernameOrEmail(), request.getRemoteAddr());
        return ResponseEntity.accepted().body(Map.of("message",
                "If the account exists, a password reset link will be sent to its registered email."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> confirm(@Valid @RequestBody PasswordResetConfirmation body,
            HttpServletRequest request) {
        service.confirm(body.token(), body.newPassword(), request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. Please sign in."));
    }
}
