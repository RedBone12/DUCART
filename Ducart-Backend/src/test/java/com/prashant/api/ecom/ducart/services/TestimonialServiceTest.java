package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Testimonial;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.TestimonialDTO;
import com.prashant.api.ecom.ducart.modal.TestimonialResponseDTO;
import com.prashant.api.ecom.ducart.repositories.TestimonialRepo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestimonialServiceTest {

    @Mock
    private TestimonialRepo testimonialRepo;

    @InjectMocks
    private TestimonialService testimonialService;

    /*
     * 这里测试重构后的 TestimonialService：DTO/Entity 转换、默认 active、
     * JSON pic URL、更新时保留旧图片，以及找不到资源时的 404 异常。
     */
    private Testimonial testimonial(
            Long id,
            String name,
            String message,
            boolean active) {

        Testimonial testimonial = new Testimonial();
        testimonial.setId(id);
        testimonial.setName(name);
        testimonial.setMessage(message);
        testimonial.setPic("/uploads/testimonials/alice.jpg");
        testimonial.setActive(active);
        return testimonial;
    }

    private TestimonialDTO testimonialDTO(
            String name,
            String message,
            Boolean active) {

        TestimonialDTO dto = new TestimonialDTO();
        dto.setName(name);
        dto.setMessage(message);
        dto.setActive(active);
        return dto;
    }

    private MultipartFile noFile() {
        return null;
    }

    @Test
    void create_whenRequestIsValid_shouldSaveAndReturnResponseDTO()
            throws Exception {

        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                true);

        when(testimonialRepo.save(any(Testimonial.class)))
                .thenAnswer(invocation -> {
                    Testimonial saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        TestimonialResponseDTO result = testimonialService.create(dto, noFile());

        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getName());
        assertEquals("Great shopping experience!", result.getMessage());
        assertTrue(result.getActive());
        assertNull(result.getPic());

        verify(testimonialRepo).save(any(Testimonial.class));
    }

    @Test
    void create_whenActiveIsNull_shouldDefaultActiveToTrue()
            throws Exception {

        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                null);

        when(testimonialRepo.save(any(Testimonial.class)))
                .thenAnswer(invocation -> {
                    Testimonial saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        TestimonialResponseDTO result = testimonialService.create(dto, noFile());

        assertTrue(result.getActive());
        verify(testimonialRepo).save(any(Testimonial.class));
    }

    @Test
    void create_whenPicUrlIsProvided_shouldKeepPicUrl()
            throws Exception {

        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                true);

        dto.setPic("/uploads/testimonials/alice.jpg");

        when(testimonialRepo.save(any(Testimonial.class)))
                .thenAnswer(invocation -> {
                    Testimonial saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        TestimonialResponseDTO result = testimonialService.create(dto, noFile());

        assertEquals(
                "/uploads/testimonials/alice.jpg",
                result.getPic());

        verify(testimonialRepo).save(any(Testimonial.class));
    }

    @Test
    void create_whenPicUrlIsBlank_shouldIgnoreIt()
            throws Exception {
        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                true);
        dto.setPic("   ");
        when(testimonialRepo.save(any(Testimonial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TestimonialResponseDTO result = testimonialService.create(dto, noFile());

        assertNull(result.getPic());
    }

    @Test
    void create_whenMultipartPicIsProvided_shouldStoreFileAndReturnPath()
            throws Exception {
        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "Great shopping experience!",
                true);
        MockMultipartFile pic = new MockMultipartFile(
                "pic",
                "alice photo.jpg",
                "image/jpeg",
                "picture".getBytes(StandardCharsets.UTF_8));
        when(testimonialRepo.save(any(Testimonial.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TestimonialResponseDTO result = testimonialService.create(dto, pic);
        Path storedFile = Path.of(
                "uploads",
                "testimonials",
                Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(result.getPic().startsWith("/uploads/testimonials/"));
            assertTrue(Files.exists(storedFile));
            assertEquals("picture", Files.readString(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void create_whenDtoOrRequiredFieldsAreNull_shouldThrowBadRequest() {
        TestimonialDTO nullName = testimonialDTO(
                null,
                "Great shopping experience!",
                true);
        TestimonialDTO nullMessage = testimonialDTO(
                "Alice",
                null,
                true);

        assertThrows(BadRequestException.class,
                () -> testimonialService.create(null, noFile()));
        assertThrows(BadRequestException.class,
                () -> testimonialService.create(nullName, noFile()));
        assertThrows(BadRequestException.class,
                () -> testimonialService.create(nullMessage, noFile()));

        verify(testimonialRepo, never()).save(any(Testimonial.class));
    }

    @Test
    void create_whenNameIsBlank_shouldThrowBadRequestException() {
        TestimonialDTO dto = testimonialDTO(
                "",
                "Great shopping experience!",
                true);

        assertThrows(
                BadRequestException.class,
                () -> testimonialService.create(dto, noFile()));

        verify(testimonialRepo, never())
                .save(any(Testimonial.class));
    }

    @Test
    void create_whenMessageIsBlank_shouldThrowBadRequestException() {
        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "",
                true);

        assertThrows(
                BadRequestException.class,
                () -> testimonialService.create(dto, noFile()));

        verify(testimonialRepo, never())
                .save(any(Testimonial.class));
    }

    @Test
    void findAll_shouldReturnResponseDTOList() {
        when(testimonialRepo.findAll()).thenReturn(List.of(
                testimonial(
                        1L,
                        "Alice",
                        "Great shopping experience!",
                        true),
                testimonial(
                        2L,
                        "Bob",
                        "Fast delivery.",
                        false)));

        List<TestimonialResponseDTO> result = testimonialService.findAll();

        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).getName());
        assertTrue(result.get(0).getActive());
        assertEquals("Bob", result.get(1).getName());
        assertFalse(result.get(1).getActive());

        verify(testimonialRepo).findAll();
    }

    @Test
    void findById_whenTestimonialExists_shouldReturnResponseDTO() {
        when(testimonialRepo.findById(1L))
                .thenReturn(Optional.of(testimonial(
                        1L,
                        "Alice",
                        "Great shopping experience!",
                        true)));

        TestimonialResponseDTO result = testimonialService.findById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getName());
        assertEquals(
                "Great shopping experience!",
                result.getMessage());

        verify(testimonialRepo).findById(1L);
    }

    @Test
    void findById_whenTestimonialDoesNotExist_shouldThrowNotFound() {
        when(testimonialRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testimonialService.findById(99L));

        verify(testimonialRepo).findById(99L);
    }

    @Test
    void update_whenNoNewPic_shouldKeepOldPic()
            throws Exception {

        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Old message",
                true);

        TestimonialDTO dto = testimonialDTO(
                "Alice Updated",
                "New message",
                false);

        when(testimonialRepo.findById(1L))
                .thenReturn(Optional.of(existing));
        when(testimonialRepo.save(existing))
                .thenReturn(existing);

        TestimonialResponseDTO result = testimonialService.update(1L, dto, noFile());

        assertEquals("Alice Updated", result.getName());
        assertEquals("New message", result.getMessage());
        assertEquals(
                "/uploads/testimonials/alice.jpg",
                result.getPic());
        assertFalse(result.getActive());

        verify(testimonialRepo).findById(1L);
        verify(testimonialRepo).save(existing);
    }

    @Test
    void update_whenActiveAndPicUrlAreOmitted_shouldKeepExistingValues()
            throws Exception {
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Old message",
                false);
        TestimonialDTO dto = testimonialDTO(
                "Alice Updated",
                "New message",
                null);
        dto.setPic(" ");
        when(testimonialRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(testimonialRepo.save(existing)).thenReturn(existing);

        TestimonialResponseDTO result = testimonialService.update(1L, dto, noFile());

        assertFalse(result.getActive());
        assertEquals("/uploads/testimonials/alice.jpg", result.getPic());
    }

    @Test
    void update_whenMultipartPicIsProvided_shouldReplacePic()
            throws Exception {
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Old message",
                true);
        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "New message",
                true);
        MockMultipartFile pic = new MockMultipartFile(
                "pic",
                "new.jpg",
                "image/jpeg",
                "new-picture".getBytes(StandardCharsets.UTF_8));
        when(testimonialRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(testimonialRepo.save(existing)).thenReturn(existing);

        TestimonialResponseDTO result = testimonialService.update(1L, dto, pic);
        Path storedFile = Path.of(
                "uploads",
                "testimonials",
                Path.of(result.getPic()).getFileName().toString());

        try {
            assertTrue(result.getPic().startsWith("/uploads/testimonials/"));
            assertTrue(Files.exists(storedFile));
        } finally {
            Files.deleteIfExists(storedFile);
        }
    }

    @Test
    void update_whenTestimonialDoesNotExist_shouldThrowNotFound() {
        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "New message",
                true);

        when(testimonialRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testimonialService.update(99L, dto, noFile()));

        verify(testimonialRepo).findById(99L);
        verify(testimonialRepo, never())
                .save(any(Testimonial.class));
    }

    @Test
    void updatePlain_whenPicIsNull_shouldKeepOldPic() {
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Old message",
                true);

        TestimonialDTO dto = testimonialDTO(
                "Alice Updated",
                "New message",
                false);

        when(testimonialRepo.findById(1L))
                .thenReturn(Optional.of(existing));
        when(testimonialRepo.save(existing))
                .thenReturn(existing);

        TestimonialResponseDTO result = testimonialService.updatePlain(1L, dto);

        assertEquals(
                "/uploads/testimonials/alice.jpg",
                result.getPic());
        assertFalse(result.getActive());

        verify(testimonialRepo).save(existing);
    }

    @Test
    void updatePlain_whenNewPicUrlIsProvided_shouldUpdatePic() {
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Old message",
                true);

        TestimonialDTO dto = testimonialDTO(
                "Alice Updated",
                "New message",
                true);

        dto.setPic("/uploads/testimonials/new-alice.jpg");

        when(testimonialRepo.findById(1L))
                .thenReturn(Optional.of(existing));
        when(testimonialRepo.save(existing))
                .thenReturn(existing);

        TestimonialResponseDTO result = testimonialService.updatePlain(1L, dto);

        assertEquals(
                "/uploads/testimonials/new-alice.jpg",
                result.getPic());

        verify(testimonialRepo).save(existing);
    }

    @Test
    void updatePlain_whenTestimonialDoesNotExist_shouldThrowNotFound() {
        TestimonialDTO dto = testimonialDTO(
                "Alice",
                "New message",
                true);
        when(testimonialRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> testimonialService.updatePlain(99L, dto));

        verify(testimonialRepo, never()).save(any(Testimonial.class));
    }

    @Test
    void delete_whenTestimonialExists_shouldDelete() {
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Great shopping experience!",
                true);

        existing.setPic(null);

        when(testimonialRepo.findById(1L))
                .thenReturn(Optional.of(existing));

        testimonialService.delete(1L);

        verify(testimonialRepo).findById(1L);
        verify(testimonialRepo).deleteById(1L);
    }

    @Test
    void delete_whenPicIsBlank_shouldOnlyDeleteDatabaseRecord() {
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Great shopping experience!",
                true);
        existing.setPic("   ");
        when(testimonialRepo.findById(1L)).thenReturn(Optional.of(existing));

        testimonialService.delete(1L);

        verify(testimonialRepo).deleteById(1L);
    }

    @Test
    void delete_whenStoredPicExists_shouldDeleteDatabaseRecordAndFile()
            throws Exception {
        Path uploadDir = Path.of("uploads", "testimonials");
        Files.createDirectories(uploadDir);
        Path storedFile = Files.createTempFile(uploadDir, "testimonial-test-", ".jpg");
        Testimonial existing = testimonial(
                1L,
                "Alice",
                "Great shopping experience!",
                true);
        existing.setPic("/uploads/testimonials/" + storedFile.getFileName());
        when(testimonialRepo.findById(1L)).thenReturn(Optional.of(existing));

        testimonialService.delete(1L);

        assertFalse(Files.exists(storedFile));
        verify(testimonialRepo).deleteById(1L);
    }

    @Test
    void delete_whenTestimonialDoesNotExist_shouldThrowNotFound() {
        when(testimonialRepo.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> testimonialService.delete(99L));

        verify(testimonialRepo).findById(99L);
        verify(testimonialRepo, never()).deleteById(99L);
    }
}
