package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.modal.AuthResponseDTO;
import com.prashant.api.ecom.ducart.modal.LoginDTO;
import com.prashant.api.ecom.ducart.repositories.UserRepo;
import com.prashant.api.ecom.ducart.security.JwtService;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import org.springframework.security.authentication.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final UserRepo userRepo;
    private final JwtService jwtService;

    public LoginController(AuthenticationManager authenticationManager,
            UserRepo userRepo,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepo = userRepo;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginDTO loginDTO) {
        if (loginDTO.getUsername() == null || loginDTO.getUsername().isBlank()) {
            throw new BadRequestException("Username or email is required");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDTO.getUsername(),
                        loginDTO.getPassword()));

        User user = userRepo.findByUsername(loginDTO.getUsername())
                .orElseGet(() -> userRepo.findByEmail(loginDTO.getUsername())
                        .orElseThrow(() -> new BadCredentialsException("Invalid username/email or password")));

        String role = user.getRole() == null ? "Buyer" : user.getRole();
        String token = jwtService.generateToken(user);

        AuthResponseDTO response = new AuthResponseDTO(
                user.getUserid(),
                user.getName(),
                user.getUsername(),
                user.getEmail(),
                role,
                token);

        return ResponseEntity.ok(response);
    }
}