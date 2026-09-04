package com.prashant.api.ecom.ducart.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.modal.TestimonialDTO;
import com.prashant.api.ecom.ducart.modal.TestimonialResponseDTO;
import com.prashant.api.ecom.ducart.services.TestimonialService;

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
@RequestMapping("/testimonial")
public class TestimonialController {

    private final TestimonialService testimonialService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public TestimonialController(
            TestimonialService testimonialService,
            ObjectMapper objectMapper,
            Validator validator) {
        this.testimonialService = testimonialService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /*
     * multipart 和 JSON 请求现在统一使用 TestimonialDTO 和 TestimonialService。
     * Controller 只处理请求解析、DTO 校验和 HTTP 响应，数据库和图片逻辑交给 Service。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestimonialResponseDTO> createMultipart(
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        TestimonialDTO dto = parseAndValidateTestimonialDTO(data);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testimonialService.create(dto, pic));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestimonialResponseDTO> createJson(
            @Valid @RequestBody TestimonialDTO dto) throws IOException {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(testimonialService.create(dto, null));
    }

    @GetMapping
    public ResponseEntity<List<TestimonialResponseDTO>> getAll() {
        return ResponseEntity.ok(testimonialService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestimonialResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(testimonialService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TestimonialResponseDTO> updateMultipart(
            @PathVariable Long id,
            @RequestPart("data") String data,
            @RequestPart(value = "pic", required = false) MultipartFile pic) throws IOException {

        TestimonialDTO dto = parseAndValidateTestimonialDTO(data);

        return ResponseEntity.ok(
                testimonialService.update(id, dto, pic));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TestimonialResponseDTO> updateJson(
            @PathVariable Long id,
            @Valid @RequestBody TestimonialDTO dto) {

        return ResponseEntity.ok(
                testimonialService.updatePlain(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        testimonialService.delete(id);

        return ResponseEntity.ok(
                Map.of("message", "Testimonial deleted successfully"));
    }

    private TestimonialDTO parseAndValidateTestimonialDTO(String data) {
        TestimonialDTO dto;

        try {
            dto = objectMapper.readValue(data, TestimonialDTO.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid testimonial data");
        }

        validateTestimonialDTO(dto);
        return dto;
    }

    private void validateTestimonialDTO(TestimonialDTO dto) {
        Set<ConstraintViolation<TestimonialDTO>> violations = validator.validate(dto);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(
                            violation -> violation.getPropertyPath().toString()))
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Invalid testimonial data");

            throw new BadRequestException(message);
        }
    }
}