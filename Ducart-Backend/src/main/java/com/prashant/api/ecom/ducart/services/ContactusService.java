package com.prashant.api.ecom.ducart.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.prashant.api.ecom.ducart.entities.Contactus;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ContactusDTO;
import com.prashant.api.ecom.ducart.modal.ContactusResponseDTO;
import com.prashant.api.ecom.ducart.repositories.ContactusRepo;

@Service
public class ContactusService {

  private final ContactusRepo contactusRepo;

  public ContactusService(ContactusRepo contactusRepo) {
    this.contactusRepo = contactusRepo;
  }

  // Save contact message
  public ContactusResponseDTO saveContactus(ContactusDTO contactusDTO) {
    Contactus contactus = new Contactus();

    // DTO.date, Entity.date, ResponseDTO.date are all LocalDate now,
    // so BeanUtils can copy the date directly.
    BeanUtils.copyProperties(contactusDTO, contactus);

    // If frontend does not send date,
    // backend sets today's date automatically.
    if (contactus.getDate() == null) {
      contactus.setDate(LocalDate.now());
    }

    Contactus savedContactus = contactusRepo.save(contactus);

    return mapToResponseDTO(savedContactus);
  }

  // Get all contact messages
  public List<ContactusResponseDTO> getAllContactus() {
    return contactusRepo.findAll()
        .stream()
        .map(this::mapToResponseDTO)
        .collect(Collectors.toList());
  }

  // Get one contact message by id
  public ContactusResponseDTO findById(Long id) {
    Contactus contactus = contactusRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Contact query not found"));

    return mapToResponseDTO(contactus);
  }

  // Update contact message
  public ContactusResponseDTO update(Long id, ContactusDTO contactusDTO) {
    Contactus existing = contactusRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Contact query not found"));

    existing.setName(contactusDTO.getName());
    existing.setEmail(contactusDTO.getEmail());
    existing.setPhone(contactusDTO.getPhone());
    existing.setSubject(contactusDTO.getSubject());
    existing.setMessage(contactusDTO.getMessage());

    // If date is not sent during update, keep the old date.
    if (contactusDTO.getDate() != null) {
      existing.setDate(contactusDTO.getDate());
    }

    existing.setActive(contactusDTO.isActive());

    Contactus updated = contactusRepo.save(existing);

    return mapToResponseDTO(updated);
  }

  // Delete contact message
  public void delete(Long id) {
    if (!contactusRepo.existsById(id)) {
      throw new ResourceNotFoundException("Contact query not found");
    }

    contactusRepo.deleteById(id);
  }

  // Convert entity to response DTO
  ContactusResponseDTO mapToResponseDTO(Contactus contactus) {
    ContactusResponseDTO contactusResponseDTO = new ContactusResponseDTO();

    // Entity.date and ResponseDTO.date are both LocalDate,
    // so no manual conversion is needed.
    BeanUtils.copyProperties(contactus, contactusResponseDTO);

    return contactusResponseDTO;
  }
}