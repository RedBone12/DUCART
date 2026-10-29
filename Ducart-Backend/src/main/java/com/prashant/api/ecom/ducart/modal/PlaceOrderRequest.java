package com.prashant.api.ecom.ducart.modal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlaceOrderRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9-]{8,64}") String idempotencyKey,
        @NotBlank @Pattern(regexp = "COD|Card Demo|PayPal Demo|Bank Transfer") String paymentMode,
        @NotEmpty @Size(max = 100) List<@NotNull @Valid Item> products) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(@NotNull @Positive Long productId, @NotNull @Positive Integer qty) {}
}
