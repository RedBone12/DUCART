package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.modal.NewsletterDTO;
import com.prashant.api.ecom.ducart.modal.NewsletterResponseDTO;
import com.prashant.api.ecom.ducart.services.NewsletterService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/newsletter")
public class NewsletterController {

    private final NewsletterService newsletterService;

    public NewsletterController(NewsletterService newsletterService) {
        this.newsletterService = newsletterService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NewsletterResponseDTO>> getAllNewsletter() {
        return ResponseEntity.ok(newsletterService.getAllNewsLetter());
    }

    @PostMapping
    public ResponseEntity<NewsletterResponseDTO> createNewsletter(@Valid @RequestBody NewsletterDTO newsletterDTO) {
        NewsletterResponseDTO saved = newsletterService.createNewsletter(newsletterDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsletterResponseDTO> getNewsletter(@PathVariable Long id) {
        return ResponseEntity.ok(newsletterService.getNewsletterById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsletterResponseDTO> updateNewsletter(@PathVariable Long id,
                                                                  @Valid @RequestBody NewsletterDTO newsletterDTO) {
        return ResponseEntity.ok(newsletterService.updateNewsletterById(id, newsletterDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteNewsletter(@PathVariable Long id) {
        newsletterService.deleteNewsletterById(id);
        return ResponseEntity.ok(Map.of("message", "Newsletter deleted successfully"));
    }
}