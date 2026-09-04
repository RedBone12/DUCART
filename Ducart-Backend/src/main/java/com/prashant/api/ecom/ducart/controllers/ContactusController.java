package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.modal.ContactusDTO;
import com.prashant.api.ecom.ducart.modal.ContactusResponseDTO;
import com.prashant.api.ecom.ducart.services.ContactusService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contactus")
public class ContactusController {

    private final ContactusService contactusService;

    public ContactusController(ContactusService contactusService) {
        this.contactusService = contactusService;
    }

    @PostMapping
    public ResponseEntity<ContactusResponseDTO> saveContactus(@Valid @RequestBody ContactusDTO contactusDTO) {
        ContactusResponseDTO saved = contactusService.saveContactus(contactusDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<ContactusResponseDTO>> getAllContactus() {
        return ResponseEntity.ok(contactusService.getAllContactus());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContactusResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contactusService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ContactusResponseDTO> update(@PathVariable Long id,
            @Valid @RequestBody ContactusDTO contactusDTO) {
        return ResponseEntity.ok(contactusService.update(id, contactusDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        contactusService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Contact query deleted successfully"));
    }
}