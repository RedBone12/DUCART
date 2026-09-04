package com.prashant.api.ecom.ducart.modal;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactusResponseDTO {
  private Long id;
  private String name;
  private String email;
  private String phone;
  private String subject;
  private String message;
  private LocalDate date;
  private boolean active;

}
