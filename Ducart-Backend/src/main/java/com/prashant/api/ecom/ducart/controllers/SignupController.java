package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.modal.ProfileDTO;
import com.prashant.api.ecom.ducart.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import com.prashant.api.ecom.ducart.modal.UserResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import com.prashant.api.ecom.ducart.modal.SignupDTO;
import com.prashant.api.ecom.ducart.modal.SignupResponseDTO;

@RestController
@RequestMapping("/user")
public class SignupController {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public SignupController(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<SignupResponseDTO> signup(@Valid @RequestBody SignupDTO signupDTO) {
        User user = new User();

        user.setName(signupDTO.getName());
        user.setUsername(signupDTO.getUsername());
        user.setEmail(signupDTO.getEmail());
        user.setPhone(signupDTO.getPhone());
        user.setPassword(signupDTO.getPassword());

        User savedUser = userService.signup(user);

        SignupResponseDTO response = SignupResponseDTO.builder()
                .userid(savedUser.getUserid())
                .name(savedUser.getName())
                .username(savedUser.getUsername())
                .email(savedUser.getEmail())
                .phone(savedUser.getPhone())
                .role(savedUser.getRole())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.findAll().stream().map(UserResponseDTO::from).toList());
    }

    @GetMapping("/{userid}")
    @PreAuthorize("hasRole('ADMIN') or #userid == authentication.details")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userid) {
        return ResponseEntity.ok(UserResponseDTO.from(userService.findById(userid)));
    }

    @PutMapping(value = "/{userid}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN') or #userid == authentication.details")
    public ResponseEntity<UserResponseDTO> updateProfile(@PathVariable Long userid,
            @RequestPart("data") String jsonData,
            @RequestPart(value = "pic", required = false) MultipartFile file) throws Exception {
        ProfileDTO profileDTO = objectMapper.readValue(jsonData, ProfileDTO.class);
        return ResponseEntity.ok(UserResponseDTO.from(userService.updateProfile(userid, profileDTO, file)));
    }

    @PutMapping("/{userid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userid, @RequestBody User user) {
        return ResponseEntity.ok(UserResponseDTO.from(userService.update(userid, user)));
    }

    @DeleteMapping("/{userid}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userid) {
        userService.delete(userid);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }
}
