package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.BrandDTO;
import com.prashant.api.ecom.ducart.modal.BrandResponseDTO;
import com.prashant.api.ecom.ducart.services.BrandService;

import jakarta.validation.ConstraintViolation;
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
@RequestMapping("/brand")
public class BrandController {

    private final BrandService brandService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public BrandController(
            BrandService brandService,
            ObjectMapper objectMapper,
            Validator validator) {
        this.brandService = brandService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BrandResponseDTO> create(
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        BrandDTO dto = parseAndValidateBrandDTO(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandService.create(dto, pic));
    }

    @GetMapping
    public ResponseEntity<List<BrandResponseDTO>> getAll() {
        return ResponseEntity.ok(brandService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrandResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BrandResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        BrandDTO dto = parseAndValidateBrandDTO(data);
        return ResponseEntity.ok(brandService.update(id, dto, pic));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BrandResponseDTO> updatePlain(
            @PathVariable Long id,
            @RequestBody BrandDTO dto) {

        validateBrandDTO(dto);
        return ResponseEntity.ok(brandService.updatePlain(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Brand deleted successfully"));
    }

    private BrandDTO parseAndValidateBrandDTO(String data) {
        BrandDTO dto;

        try {
            dto = objectMapper.readValue(data, BrandDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid brand data");
        }

        validateBrandDTO(dto);
        return dto;
    }

    private void validateBrandDTO(BrandDTO dto) {
        Set<ConstraintViolation<BrandDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid brand data");

            throw new BadRequestException(message);
        }
    }
}