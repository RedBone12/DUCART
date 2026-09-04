package com.prashant.api.ecom.ducart.modal;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordDTO {
    private String usernameOrEmail;
    private String phone;
    private String newPassword;
}