package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.SubcategoryDTO;
import com.prashant.api.ecom.ducart.modal.SubcategoryResponseDTO;
import com.prashant.api.ecom.ducart.services.SubcategoryService;

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
@RequestMapping("/subcategory")
public class SubcategoryController {

    private final SubcategoryService subcategoryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SubcategoryController(
            SubcategoryService subcategoryService,
            ObjectMapper objectMapper,
            Validator validator) {
        this.subcategoryService = subcategoryService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /*
     * Controller 只负责解析 multipart 里的 data、执行 DTO 校验、调用 SubcategoryService。
     * 删除前检查商品引用、图片保存、实体转换都放在 Service 里。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubcategoryResponseDTO> create(
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        SubcategoryDTO dto = parseAndValidateSubcategoryDTO(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subcategoryService.create(dto, pic));
    }

    @GetMapping
    public ResponseEntity<List<SubcategoryResponseDTO>> getAll() {
        return ResponseEntity.ok(subcategoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubcategoryResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(subcategoryService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubcategoryResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        SubcategoryDTO dto = parseAndValidateSubcategoryDTO(data);
        return ResponseEntity.ok(subcategoryService.update(id, dto, pic));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubcategoryResponseDTO> updatePlain(
            @PathVariable Long id,
            @RequestBody SubcategoryDTO dto) {

        validateSubcategoryDTO(dto);
        return ResponseEntity.ok(subcategoryService.updatePlain(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        subcategoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Subcategory deleted successfully"));
    }

    private SubcategoryDTO parseAndValidateSubcategoryDTO(String data) {
        SubcategoryDTO dto;

        try {
            dto = objectMapper.readValue(data, SubcategoryDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid subcategory data");
        }

        validateSubcategoryDTO(dto);
        return dto;
    }

    private void validateSubcategoryDTO(SubcategoryDTO dto) {
        Set<ConstraintViolation<SubcategoryDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid subcategory data");

            throw new BadRequestException(message);
        }
    }
}