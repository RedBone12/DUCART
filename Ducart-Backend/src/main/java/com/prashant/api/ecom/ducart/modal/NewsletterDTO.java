package com.prashant.api.ecom.ducart.modal;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterDTO {
  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  private String email;
  private Boolean active;

}
