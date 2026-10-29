package com.prashant.api.ecom.ducart.modal;

import jakarta.validation.constraints.*;

public record PasswordResetConfirmation(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
        @NotBlank @Size(min = 12, max = 72) String newPassword) {}
