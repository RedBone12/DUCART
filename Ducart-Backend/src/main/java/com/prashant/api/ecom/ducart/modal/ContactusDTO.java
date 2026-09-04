package com.prashant.api.ecom.ducart.modal;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Email;

@Data
public class ContactusDTO {

  @NotBlank(message = "Name is required")
  private String name;
  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  private String email;
  @NotBlank(message = "Phone Number is required")
  private String phone;
  @NotBlank(message = "Subject is required")
  private String subject;
  @NotBlank(message = "Message is required")
  private String message;
   // Use @NotNull for LocalDate.
  // @NotBlank is only for String.
  // @NotNull(message = "Date is required")
  private LocalDate date;
  private boolean active;

}
