package com.prashant.api.ecom.ducart.modal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown =  true)
public class SubcategoryDTO {
  @NotBlank(message = "Subcategory name is required")
  private String name;
  // @NotBlank(message = "Pic is required")
  // private String pic;
  private Boolean active;

}
