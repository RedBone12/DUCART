package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Testimonial;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.TestimonialDTO;
import com.prashant.api.ecom.ducart.modal.TestimonialResponseDTO;
import com.prashant.api.ecom.ducart.repositories.TestimonialRepo;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class TestimonialService {

  private final TestimonialRepo testimonialRepo;
  private final String uploadDir = FileUploadUtil.getUploadDirFor("testimonials");

  public TestimonialService(TestimonialRepo testimonialRepo) {
    this.testimonialRepo = testimonialRepo;
  }

  public TestimonialResponseDTO create(
      TestimonialDTO dto,
      MultipartFile pic) throws IOException {

    validateTestimonialDTO(dto);

    Testimonial testimonial = new Testimonial();
    applyTestimonialDTO(testimonial, dto, true);

    String savedPic = FileUploadUtil.save(pic, "testimonials");
    if (savedPic != null) {
      testimonial.setPic(savedPic);
    }

    return toResponseDTO(testimonialRepo.save(testimonial));
  }

  public List<TestimonialResponseDTO> findAll() {
    return testimonialRepo.findAll()
        .stream()
        .map(this::toResponseDTO)
        .toList();
  }

  public TestimonialResponseDTO findById(Long id) {
    return toResponseDTO(findEntityById(id));
  }

  public TestimonialResponseDTO update(
      Long id,
      TestimonialDTO dto,
      MultipartFile pic) throws IOException {

    validateTestimonialDTO(dto);

    Testimonial existing = findEntityById(id);
    applyTestimonialDTO(existing, dto, false);

    String savedPic = FileUploadUtil.save(pic, "testimonials");
    if (savedPic != null) {
      existing.setPic(savedPic);
    }

    return toResponseDTO(testimonialRepo.save(existing));
  }

  public TestimonialResponseDTO updatePlain(Long id, TestimonialDTO dto) {
    validateTestimonialDTO(dto);

    Testimonial existing = findEntityById(id);
    applyTestimonialDTO(existing, dto, false);

    return toResponseDTO(testimonialRepo.save(existing));
  }

  public void delete(Long id) {
    Testimonial testimonial = findEntityById(id);
    String oldPic = testimonial.getPic();

    testimonialRepo.deleteById(id);

    if (oldPic != null && !oldPic.isBlank()) {
      deleteFile(oldPic);
    }
  }

  private Testimonial findEntityById(Long id) {
    return testimonialRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found"));
  }

  private void validateTestimonialDTO(TestimonialDTO dto) {
    if (dto == null) {
      throw new BadRequestException("Testimonial data is required");
    }

    if (dto.getName() == null || dto.getName().isBlank()) {
      throw new BadRequestException("Name is required");
    }

    if (dto.getMessage() == null || dto.getMessage().isBlank()) {
      throw new BadRequestException("Message is required");
    }
  }

  private void applyTestimonialDTO(Testimonial testimonial, TestimonialDTO dto, boolean creating) {
    testimonial.setName(dto.getName());
    testimonial.setMessage(dto.getMessage());

    if (dto.getPic() != null && !dto.getPic().isBlank()) {
      testimonial.setPic(dto.getPic());
    }

    if (dto.getActive() != null) {
      testimonial.setActive(dto.getActive());
    } else if (creating) {
      testimonial.setActive(true);
    }
  }

  private TestimonialResponseDTO toResponseDTO(Testimonial testimonial) {
    return TestimonialResponseDTO.builder()
        .id(testimonial.getId())
        .name(testimonial.getName())
        .message(testimonial.getMessage())
        .pic(testimonial.getPic())
        .active(testimonial.isActive())
        .build();
  }

  private void deleteFile(String filePath) {
    try {
      Path path = Path.of(uploadDir, new File(filePath).getName());
      Files.deleteIfExists(path);
    } catch (IOException ex) {
      System.err.println(
          "Error deleting file: " + filePath + " - " + ex.getMessage());
    }
  }
}