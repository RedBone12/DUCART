package com.prashant.api.ecom.ducart.modal;

import jakarta.validation.constraints.*;

public record PasswordResetRequest(@NotBlank @Size(max = 254) String usernameOrEmail) {}
