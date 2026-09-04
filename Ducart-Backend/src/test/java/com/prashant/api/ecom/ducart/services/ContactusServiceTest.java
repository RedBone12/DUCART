package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Contactus;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ContactusDTO;
import com.prashant.api.ecom.ducart.modal.ContactusResponseDTO;
import com.prashant.api.ecom.ducart.repositories.ContactusRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactusServiceTest {

    @Mock
    private ContactusRepo contactusRepo;

    @InjectMocks
    private ContactusService contactusService;

    private Contactus contactus() {
        // This fake entity represents a contact message record from the database.
        Contactus contactus = new Contactus();
        contactus.setId(1L);
        contactus.setName("Alice");
        contactus.setEmail("alice@test.com");
        contactus.setPhone("1234567890");
        contactus.setSubject("Order question");
        contactus.setMessage("I want to ask about my order.");
        contactus.setDate(LocalDate.of(2026, 6, 30));
        contactus.setActive(true);
        return contactus;
    }

    private ContactusDTO contactusDTO() {
        // This fake DTO represents data sent from the frontend contact form.
        ContactusDTO dto = new ContactusDTO();
        dto.setName("Alice");
        dto.setEmail("alice@test.com");
        dto.setPhone("1234567890");
        dto.setSubject("Order question");
        dto.setMessage("I want to ask about my order.");
        dto.setDate(LocalDate.of(2026, 6, 30));
        dto.setActive(true);
        return dto;
    }

    @Test
    void saveContactus_shouldSaveContactMessageAndReturnResponseDTO() {
        ContactusDTO dto = contactusDTO();

        // Mock save().
        // Instead of saving to the real database,
        // Mockito returns the Contactus object passed into save().
        when(contactusRepo.save(any(Contactus.class))).thenAnswer(invocation -> {
            Contactus savedContactus = invocation.getArgument(0);
            savedContactus.setId(1L);
            return savedContactus;
        });

        ContactusResponseDTO result = contactusService.saveContactus(dto);

        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getName());
        assertEquals("alice@test.com", result.getEmail());
        assertEquals("1234567890", result.getPhone());
        assertEquals("Order question", result.getSubject());
        assertEquals("I want to ask about my order.", result.getMessage());
        assertEquals(LocalDate.of(2026, 6, 30), result.getDate());
        assertTrue(result.isActive());

        verify(contactusRepo).save(any(Contactus.class));
    }

    @Test
    void saveContactus_whenDateIsNull_shouldSetCurrentDateAutomatically() {
        ContactusDTO dto = contactusDTO();
        dto.setDate(null);

        // Mock save().
        // The service should set today's date before saving.
        when(contactusRepo.save(any(Contactus.class))).thenAnswer(invocation -> {
            Contactus savedContactus = invocation.getArgument(0);
            savedContactus.setId(1L);
            return savedContactus;
        });

        ContactusResponseDTO result = contactusService.saveContactus(dto);

        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getName());
        assertNotNull(result.getDate());
        assertEquals(LocalDate.now(), result.getDate());

        verify(contactusRepo).save(any(Contactus.class));
    }

    @Test
    void getAllContactus_shouldReturnContactusResponseDTOList() {
        Contactus contactus = contactus();

        // Mock findAll().
        // Pretend the database has one contact message.
        when(contactusRepo.findAll()).thenReturn(List.of(contactus));

        List<ContactusResponseDTO> result = contactusService.getAllContactus();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Alice", result.get(0).getName());
        assertEquals("alice@test.com", result.get(0).getEmail());
        assertEquals("1234567890", result.get(0).getPhone());
        assertEquals("Order question", result.get(0).getSubject());
        assertEquals("I want to ask about my order.", result.get(0).getMessage());
        assertEquals(LocalDate.of(2026, 6, 30), result.get(0).getDate());
        assertTrue(result.get(0).isActive());

        verify(contactusRepo).findAll();
    }

    @Test
    void findById_whenContactusExists_shouldReturnContactusResponseDTO() {
        Contactus contactus = contactus();

        // Mock findById().
        // Pretend the contact message exists.
        when(contactusRepo.findById(1L)).thenReturn(Optional.of(contactus));

        ContactusResponseDTO result = contactusService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getName());
        assertEquals("alice@test.com", result.getEmail());
        assertEquals("1234567890", result.getPhone());
        assertEquals("Order question", result.getSubject());
        assertEquals("I want to ask about my order.", result.getMessage());
        assertEquals(LocalDate.of(2026, 6, 30), result.getDate());
        assertTrue(result.isActive());

        verify(contactusRepo).findById(1L);
    }

    @Test
    void findById_whenContactusDoesNotExist_shouldThrowResourceNotFoundException() {
        // Mock findById().
        // Pretend the contact message does not exist.
        when(contactusRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> contactusService.findById(99L));

        verify(contactusRepo).findById(99L);
    }

    @Test
    void update_whenContactusExists_shouldUpdateAndReturnResponseDTO() {
        Contactus existing = contactus();

        ContactusDTO dto = new ContactusDTO();
        dto.setName("Bob");
        dto.setEmail("bob@test.com");
        dto.setPhone("9999999999");
        dto.setSubject("Refund question");
        dto.setMessage("I want to ask about a refund.");
        dto.setDate(LocalDate.of(2026, 7, 1));
        dto.setActive(false);

        // Mock findById().
        // Pretend the contact message exists.
        when(contactusRepo.findById(1L)).thenReturn(Optional.of(existing));

        // Mock save().
        // Return the updated entity.
        when(contactusRepo.save(existing)).thenReturn(existing);

        ContactusResponseDTO result = contactusService.update(1L, dto);

        assertEquals(1L, result.getId());
        assertEquals("Bob", result.getName());
        assertEquals("bob@test.com", result.getEmail());
        assertEquals("9999999999", result.getPhone());
        assertEquals("Refund question", result.getSubject());
        assertEquals("I want to ask about a refund.", result.getMessage());
        assertEquals(LocalDate.of(2026, 7, 1), result.getDate());
        assertFalse(result.isActive());

        verify(contactusRepo).findById(1L);
        verify(contactusRepo).save(existing);
    }

    @Test
    void update_whenDateIsNull_shouldKeepOldDate() {
        Contactus existing = contactus();

        ContactusDTO dto = new ContactusDTO();
        dto.setName("Bob");
        dto.setEmail("bob@test.com");
        dto.setPhone("9999999999");
        dto.setSubject("Refund question");
        dto.setMessage("I want to ask about a refund.");
        dto.setDate(null);
        dto.setActive(false);

        // Mock findById().
        // Pretend the contact message exists.
        when(contactusRepo.findById(1L)).thenReturn(Optional.of(existing));

        // Mock save().
        // Return the updated entity.
        when(contactusRepo.save(existing)).thenReturn(existing);

        ContactusResponseDTO result = contactusService.update(1L, dto);

        assertEquals("Bob", result.getName());
        assertEquals(LocalDate.of(2026, 6, 30), result.getDate());
        assertFalse(result.isActive());

        verify(contactusRepo).findById(1L);
        verify(contactusRepo).save(existing);
    }

    @Test
    void update_whenContactusDoesNotExist_shouldThrowResourceNotFoundException() {
        ContactusDTO dto = contactusDTO();

        // Mock findById().
        // Pretend the contact message does not exist.
        when(contactusRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> contactusService.update(99L, dto));

        verify(contactusRepo).findById(99L);
        verify(contactusRepo, never()).save(any(Contactus.class));
    }

    @Test
    void delete_whenContactusExists_shouldDeleteContactus() {
        // Mock existsById().
        // Pretend the contact message exists.
        when(contactusRepo.existsById(1L)).thenReturn(true);

        contactusService.delete(1L);

        verify(contactusRepo).existsById(1L);
        verify(contactusRepo).deleteById(1L);
    }

    @Test
    void delete_whenContactusDoesNotExist_shouldThrowResourceNotFoundException() {
        // Mock existsById().
        // Pretend the contact message does not exist.
        when(contactusRepo.existsById(99L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> contactusService.delete(99L));

        verify(contactusRepo).existsById(99L);
        verify(contactusRepo, never()).deleteById(99L);
    }
}