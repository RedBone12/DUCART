package com.prashant.api.ecom.ducart.services;

import java.util.List;
import java.util.Objects;

import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.prashant.api.ecom.ducart.entities.Newsletter;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.NewsletterDTO;
import com.prashant.api.ecom.ducart.modal.NewsletterResponseDTO;
import com.prashant.api.ecom.ducart.repositories.NewsletterRepo;

@Service
public class NewsletterService {
  // @Autowired
  private final NewsletterRepo newsletterRepo;

  public NewsletterService(NewsletterRepo newsletterRepo) {
    this.newsletterRepo = newsletterRepo;
  }

  public NewsletterResponseDTO createNewsletter(NewsletterDTO newsletterDTO) {
    String email = normalizeEmail(newsletterDTO);
    if (newsletterRepo.findByEmail(email).isPresent()) {
      throw new ConflictException("Email is already subscribed");
    }

    Newsletter newsletter = new Newsletter();
    // active is nullable in the request DTO but primitive in the entity, so copy it
    // explicitly below after applying the default value.
    BeanUtils.copyProperties(newsletterDTO, newsletter, "active");
    newsletter.setEmail(email);
    if (newsletterDTO.getActive() == null) {
      newsletter.setActive(true);
    } else {
      newsletter.setActive(newsletterDTO.getActive());
    }
    Newsletter savedNewsletter = newsletterRepo.save(newsletter);
    return mapToResponseDTO(savedNewsletter);
  }

  // Helper method map entiy to ResponseDTO
  private NewsletterResponseDTO mapToResponseDTO(Newsletter newsletter) {
    NewsletterResponseDTO newsletterResponseDTO = new NewsletterResponseDTO();
    BeanUtils.copyProperties(newsletter, newsletterResponseDTO);
    return newsletterResponseDTO;
  }

  // Get all subscriptions
  public List<NewsletterResponseDTO> getAllNewsLetter() {
    // entity list in ResponseNewsletterDto
    List<Newsletter> allNewslatters = newsletterRepo.findAll();
    List<NewsletterResponseDTO> allNewslattersDTO = allNewslatters.stream().map(this::mapToResponseDTO)
        .collect(Collectors.toList());
    return allNewslattersDTO;
  }

  // Get subscription by id
  public NewsletterResponseDTO getNewsletterById(Long id) {
    Newsletter newsletter = newsletterRepo.findById(id)
        .orElseThrow(() -> new RuntimeException("Newsletter not found by id:" + id));
    NewsletterResponseDTO newsletterResponseDTO = mapToResponseDTO(newsletter);
    return newsletterResponseDTO;
  }

  // Update newsletter subscription
  public NewsletterResponseDTO updateNewsletterById(Long id, NewsletterDTO newsletterDTO) {
    Newsletter existing = newsletterRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Newsletter not found"));

    String email = normalizeEmail(newsletterDTO);
    newsletterRepo.findByEmail(email)
        .filter(newsletter -> !Objects.equals(newsletter.getId(), id))
        .ifPresent(newsletter -> {
          throw new ConflictException("Email is already subscribed");
        });
    existing.setEmail(email);
    // If active is not sent, keep old active value.
    // If active is sent, update it.
    if (newsletterDTO.getActive() != null) {
      existing.setActive(newsletterDTO.getActive());
    }
    Newsletter updated = newsletterRepo.save(existing);
    return mapToResponseDTO(updated);
  }

  private String normalizeEmail(NewsletterDTO newsletterDTO) {
    if (newsletterDTO == null) {
      throw new BadRequestException("Newsletter data is required");
    }

    String email = newsletterDTO.getEmail();
    if (email == null || email.isBlank()) {
      throw new BadRequestException("Email is required");
    }

    return email.trim().toLowerCase();
  }

  // Delete subscription by id
  public void deleteNewsletterById(Long id) {
    if (!newsletterRepo.existsById(id)) {
      throw new ResourceNotFoundException("Newsletter not found");
    }
    newsletterRepo.deleteById(id);
  }

}
