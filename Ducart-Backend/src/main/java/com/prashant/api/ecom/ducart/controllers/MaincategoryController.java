package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.MainResponseDTO;
import com.prashant.api.ecom.ducart.modal.MaincategoryDTO;
import com.prashant.api.ecom.ducart.services.MaincategoryService;

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
@RequestMapping("/maincategory")
public class MaincategoryController {

    private final MaincategoryService maincategoryService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MaincategoryController(
            MaincategoryService maincategoryService,
            ObjectMapper objectMapper,
            Validator validator) {
        this.maincategoryService = maincategoryService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /*
     * Controller 只负责解析 multipart 里的 data、执行 DTO 校验、调用 MaincategoryService。
     * 删除前检查商品引用、图片保存、实体转换都放在 Service 里。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MainResponseDTO> create(
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        MaincategoryDTO dto = parseAndValidateMaincategoryDTO(data);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(maincategoryService.create(dto, pic));
    }

    @GetMapping
    public ResponseEntity<List<MainResponseDTO>> getAll() {
        return ResponseEntity.ok(maincategoryService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MainResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(maincategoryService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MainResponseDTO> update(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        MaincategoryDTO dto = parseAndValidateMaincategoryDTO(data);
        return ResponseEntity.ok(maincategoryService.update(id, dto, pic));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MainResponseDTO> updatePlain(
            @PathVariable Long id,
            @RequestBody MaincategoryDTO dto) {

        validateMaincategoryDTO(dto);
        return ResponseEntity.ok(maincategoryService.updatePlain(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        maincategoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Main category deleted successfully"));
    }

    private MaincategoryDTO parseAndValidateMaincategoryDTO(String data) {
        MaincategoryDTO dto;

        try {
            dto = objectMapper.readValue(data, MaincategoryDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid main category data");
        }

        validateMaincategoryDTO(dto);
        return dto;
    }

    private void validateMaincategoryDTO(MaincategoryDTO dto) {
        Set<ConstraintViolation<MaincategoryDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(v -> v.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid main category data");

            throw new BadRequestException(message);
        }
    }
}