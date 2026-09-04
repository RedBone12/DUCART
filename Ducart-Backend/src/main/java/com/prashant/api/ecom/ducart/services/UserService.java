package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ProfileDTO;
import com.prashant.api.ecom.ducart.repositories.UserRepo;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.*;
import com.prashant.api.ecom.ducart.modal.ForgotPasswordDTO;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;

@Service
public class UserService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(User user) {
        if (user == null) {
            throw new BadRequestException("User data is required");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            throw new BadRequestException("Name is required");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BadRequestException("Username is required");
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException("Email is required");
        }

        if (!user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new BadRequestException("Invalid email format");
        }

        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BadRequestException("Phone number is required");
        }

        if (!user.getPhone().matches("^\\+?[0-9]{7,15}$")) {
            throw new BadRequestException("Invalid phone number format");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
        }

        if (userRepo.findByUsername(user.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists");
        }

        if (userRepo.findByEmail(user.getEmail()).isPresent()) {
            throw new ConflictException("Email already exists");
        }

        List<User> allUsers = userRepo.findAll();
        boolean phoneExists = allUsers.stream().anyMatch(u -> u.getPhone().equals(user.getPhone()));
        if (phoneExists) {
            throw new ConflictException("Phone number already exists");
        }

        if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole("Buyer");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public User findById(Long id) {
        return userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // public Optional<User> login(String usernameOrEmail, String password) {
    //     if (usernameOrEmail == null || usernameOrEmail.isBlank()) {
    //         return Optional.empty();
    //     }

    //     if (password == null || password.isBlank()) {
    //         return Optional.empty();
    //     }

    //     Optional<User> userOptional = userRepo.findByUsername(usernameOrEmail);

    //     if (userOptional.isEmpty()) {
    //         userOptional = userRepo.findByEmail(usernameOrEmail);
    //     }

    //     if (userOptional.isEmpty()) {
    //         return Optional.empty();
    //     }

    //     User user = userOptional.get();

    //     if (!user.getPassword().equals(password)) {
    //         return Optional.empty();
    //     }

    //     return Optional.of(user);
    // }

    public User updateProfile(Long id, ProfileDTO dto, MultipartFile pic) throws IOException {
        User user = findById(id);
        if (dto.getName() != null)
            user.setName(dto.getName());
        if (dto.getPhone() != null)
            user.setPhone(dto.getPhone());
        if (dto.getAddress() != null)
            user.setAddress(dto.getAddress());
        if (dto.getCity() != null)
            user.setCity(dto.getCity());
        if (dto.getState() != null)
            user.setState(dto.getState());
        if (dto.getPin() != null)
            user.setPin(dto.getPin());
        String savedPic = FileUploadUtil.save(pic, "users");
        if (savedPic != null)
            user.setPic(savedPic);
        return userRepo.save(user);
    }

    public User resetPassword(ForgotPasswordDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Password reset data is required");
        }

        if (dto.getUsernameOrEmail() == null || dto.getUsernameOrEmail().isBlank()) {
            throw new BadRequestException("Username or email is required");
        }

        if (dto.getPhone() == null || dto.getPhone().isBlank()) {
            throw new BadRequestException("Phone number is required");
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new BadRequestException("New password is required");
        }

        User user = userRepo.findByUsername(dto.getUsernameOrEmail())
                .orElseGet(() -> userRepo.findByEmail(dto.getUsernameOrEmail())
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        if (!user.getPhone().equals(dto.getPhone())) {
            throw new BadRequestException("Phone number does not match this account");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        return userRepo.save(user);
    }

    public User update(Long id, User incoming) {
        User user = findById(id);
        BeanUtils.copyProperties(incoming, user, "userid", "password");
        return userRepo.save(user);
    }

    public void delete(Long id) {
        if (!userRepo.existsById(id))
            throw new ResourceNotFoundException("User not found");
        userRepo.deleteById(id);
    }
}
