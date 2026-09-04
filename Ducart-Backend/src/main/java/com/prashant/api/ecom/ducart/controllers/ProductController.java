package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.ProductDTO;
import com.prashant.api.ecom.ducart.modal.ProductResponseDTO;
import com.prashant.api.ecom.ducart.modal.ProductStockUpdateDTO;
import com.prashant.api.ecom.ducart.services.ProductService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ProductController(
            ProductService productService,
            ObjectMapper objectMapper,
            Validator validator) {

        this.productService = productService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDTO> createProduct(
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile[] files) throws IOException {

        ProductDTO productDTO = parseAndValidateProductDTO(data);

        ProductResponseDTO createdProduct = productService.create(productDTO, files);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProduct);
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDTO> updateProductById(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile[] files) throws IOException {

        ProductDTO productDTO = parseAndValidateProductDTO(data);

        return ResponseEntity.ok(
                productService.update(id, productDTO, files));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductResponseDTO> updateProductStock(
            @PathVariable Long id,
            @Valid @RequestBody ProductStockUpdateDTO stockDTO) {

        return ResponseEntity.ok(
                productService.updateStock(id, stockDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteById(@PathVariable Long id) {
        productService.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Product deleted successfully"));
    }

    private ProductDTO parseAndValidateProductDTO(String data) {
        ProductDTO productDTO;

        try {
            productDTO = objectMapper.readValue(data, ProductDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid product data");
        }

        Set<ConstraintViolation<ProductDTO>> violations = validator.validate(productDTO);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(
                            violation -> violation.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid product data");

            throw new BadRequestException(message);
        }

        return productDTO;
    }
}