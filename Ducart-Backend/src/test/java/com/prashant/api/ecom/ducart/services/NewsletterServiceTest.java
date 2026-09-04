package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Newsletter;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.NewsletterDTO;
import com.prashant.api.ecom.ducart.modal.NewsletterResponseDTO;
import com.prashant.api.ecom.ducart.repositories.NewsletterRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock
    private NewsletterRepo newsletterRepo;

    @InjectMocks
    private NewsletterService newsletterService;

    private Newsletter newsletter() {
        // This fake entity represents a newsletter subscription record from the
        // database.
        Newsletter newsletter = new Newsletter();
        newsletter.setId(1L);
        newsletter.setEmail("alice@test.com");
        newsletter.setActive(true);
        return newsletter;
    }

    private NewsletterDTO newsletterDTO() {
        // This fake DTO represents data sent from the frontend newsletter form.
        NewsletterDTO dto = new NewsletterDTO();
        dto.setEmail("alice@test.com");
        dto.setActive(true);
        return dto;
    }

    @Test
    void createNewsletter_shouldSaveNewsletterAndReturnResponseDTO() {
        NewsletterDTO dto = newsletterDTO();

        // Mock save().
        // Instead of saving to the real database,
        // Mockito returns the Newsletter object passed into save().
        when(newsletterRepo.save(any(Newsletter.class))).thenAnswer(invocation -> {
            Newsletter savedNewsletter = invocation.getArgument(0);
            savedNewsletter.setId(1L);
            return savedNewsletter;
        });

        NewsletterResponseDTO result = newsletterService.createNewsletter(dto);

        assertEquals(1L, result.getId());
        assertEquals("alice@test.com", result.getEmail());
        assertTrue(result.isActive());

        verify(newsletterRepo).save(any(Newsletter.class));
    }

    @Test
    void createNewsletter_whenActiveIsOmitted_shouldNormalizeEmailAndDefaultToActive() {
        NewsletterDTO dto = new NewsletterDTO();
        dto.setEmail("  Alice@Test.COM  ");

        when(newsletterRepo.save(any(Newsletter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NewsletterResponseDTO result = newsletterService.createNewsletter(dto);

        assertEquals("alice@test.com", result.getEmail());
        assertTrue(result.isActive());
        verify(newsletterRepo).findByEmail("alice@test.com");
    }

    @Test
    void createNewsletter_whenRequestOrEmailIsMissing_shouldRejectIt() {
        NewsletterDTO nullEmail = new NewsletterDTO();
        NewsletterDTO blankEmail = new NewsletterDTO();
        blankEmail.setEmail("   ");

        assertThrows(BadRequestException.class,
                () -> newsletterService.createNewsletter(null));
        assertThrows(BadRequestException.class,
                () -> newsletterService.createNewsletter(nullEmail));
        assertThrows(BadRequestException.class,
                () -> newsletterService.createNewsletter(blankEmail));

        verify(newsletterRepo, never()).save(any(Newsletter.class));
    }

    @Test
    void createNewsletter_whenEmailAlreadyExists_shouldThrowConflict() {
        NewsletterDTO dto = newsletterDTO();
        when(newsletterRepo.findByEmail("alice@test.com"))
                .thenReturn(Optional.of(newsletter()));

        assertThrows(ConflictException.class,
                () -> newsletterService.createNewsletter(dto));

        verify(newsletterRepo, never()).save(any(Newsletter.class));
    }

    @Test
    void getAllNewsLetter_shouldReturnNewsletterResponseDTOList() {
        Newsletter newsletter = newsletter();

        // Mock findAll().
        // Pretend the database has one newsletter subscription.
        when(newsletterRepo.findAll()).thenReturn(List.of(newsletter));

        List<NewsletterResponseDTO> result = newsletterService.getAllNewsLetter();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("alice@test.com", result.get(0).getEmail());
        assertTrue(result.get(0).isActive());

        verify(newsletterRepo).findAll();
    }

    @Test
    void getNewsletterById_whenNewsletterExists_shouldReturnNewsletterResponseDTO() {
        Newsletter newsletter = newsletter();

        // Mock findById().
        // Pretend the newsletter subscription exists.
        when(newsletterRepo.findById(1L)).thenReturn(Optional.of(newsletter));

        NewsletterResponseDTO result = newsletterService.getNewsletterById(1L);

        assertEquals(1L, result.getId());
        assertEquals("alice@test.com", result.getEmail());
        assertTrue(result.isActive());

        verify(newsletterRepo).findById(1L);
    }

    @Test
    void getNewsletterById_whenNewsletterDoesNotExist_shouldThrowRuntimeException() {
        // Mock findById().
        // Pretend the newsletter subscription does not exist.
        when(newsletterRepo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> newsletterService.getNewsletterById(99L));

        assertEquals("Newsletter not found by id:99", exception.getMessage());

        verify(newsletterRepo).findById(99L);
    }

    @Test
    void deleteNewsletterById_whenNewsletterExists_shouldDeleteNewsletter() {
        // Mock existsById().
        // Pretend the newsletter subscription exists.
        when(newsletterRepo.existsById(1L)).thenReturn(true);

        newsletterService.deleteNewsletterById(1L);

        verify(newsletterRepo).existsById(1L);
        verify(newsletterRepo).deleteById(1L);
    }

    @Test
    void deleteNewsletterById_whenNewsletterDoesNotExist_shouldThrowResourceNotFoundException() {
        // Mock existsById().
        // Pretend the newsletter subscription does not exist.
        when(newsletterRepo.existsById(99L)).thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> newsletterService.deleteNewsletterById(99L));

        verify(newsletterRepo).existsById(99L);
        verify(newsletterRepo, never()).deleteById(99L);
    }

    @Test
    void updateNewsletterById_whenNewsletterExists_shouldUpdateNewsletter() {
        Newsletter existing = newsletter();

        NewsletterDTO dto = new NewsletterDTO();
        dto.setEmail("Bob@Test.com");
        dto.setActive(false);

        when(newsletterRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(newsletterRepo.save(existing)).thenReturn(existing);

        NewsletterResponseDTO result = newsletterService.updateNewsletterById(1L, dto);

        assertEquals("bob@test.com", result.getEmail());
        assertFalse(result.isActive());

        verify(newsletterRepo).findById(1L);
        verify(newsletterRepo).save(existing);
    }

    @Test
    void updateNewsletterById_whenActiveIsOmitted_shouldKeepExistingValue() {
        Newsletter existing = newsletter();
        existing.setActive(false);
        NewsletterDTO dto = new NewsletterDTO();
        dto.setEmail(" Updated@Test.com ");

        when(newsletterRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(newsletterRepo.save(existing)).thenReturn(existing);

        NewsletterResponseDTO result = newsletterService.updateNewsletterById(1L, dto);

        assertEquals("updated@test.com", result.getEmail());
        assertFalse(result.isActive());
    }

    @Test
    void updateNewsletterById_whenEmailBelongsToAnotherSubscription_shouldThrowConflict() {
        Newsletter existing = newsletter();
        Newsletter duplicate = newsletter();
        duplicate.setId(2L);
        NewsletterDTO dto = newsletterDTO();

        when(newsletterRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(newsletterRepo.findByEmail("alice@test.com"))
                .thenReturn(Optional.of(duplicate));

        assertThrows(ConflictException.class,
                () -> newsletterService.updateNewsletterById(1L, dto));

        verify(newsletterRepo, never()).save(any(Newsletter.class));
    }

    @Test
    void updateNewsletterById_whenEmailBelongsToSameSubscription_shouldAllowUpdate() {
        Newsletter existing = newsletter();
        NewsletterDTO dto = newsletterDTO();

        when(newsletterRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(newsletterRepo.findByEmail("alice@test.com"))
                .thenReturn(Optional.of(existing));
        when(newsletterRepo.save(existing)).thenReturn(existing);

        NewsletterResponseDTO result = newsletterService.updateNewsletterById(1L, dto);

        assertEquals("alice@test.com", result.getEmail());
        verify(newsletterRepo).save(existing);
    }

    @Test
    void updateNewsletterById_whenDtoOrEmailIsInvalid_shouldThrowBadRequest() {
        Newsletter existing = newsletter();
        NewsletterDTO blankEmail = new NewsletterDTO();
        blankEmail.setEmail(" ");
        when(newsletterRepo.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class,
                () -> newsletterService.updateNewsletterById(1L, null));
        assertThrows(BadRequestException.class,
                () -> newsletterService.updateNewsletterById(1L, blankEmail));

        verify(newsletterRepo, never()).save(any(Newsletter.class));
    }

    @Test
    void updateNewsletterById_whenNewsletterDoesNotExist_shouldThrowResourceNotFoundException() {
        NewsletterDTO dto = newsletterDTO();

        when(newsletterRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> newsletterService.updateNewsletterById(99L, dto));

        verify(newsletterRepo).findById(99L);
        verify(newsletterRepo, never()).save(any(Newsletter.class));
    }
}
