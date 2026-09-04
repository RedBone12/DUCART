package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Subcategory;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.SubcategoryDTO;
import com.prashant.api.ecom.ducart.modal.SubcategoryResponseDTO;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.repositories.SubcategoryRepo;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class SubcategoryService {

  private final SubcategoryRepo subcategoryRepo;
  private final ProductRepo productRepo;

  private final String uploadDir = FileUploadUtil.getUploadDirFor("subcategories");

  public SubcategoryService(SubcategoryRepo subcategoryRepo, ProductRepo productRepo) {
    this.subcategoryRepo = subcategoryRepo;
    this.productRepo = productRepo;
  }

  public SubcategoryResponseDTO create(SubcategoryDTO dto, MultipartFile pic) throws IOException {
    validateSubcategoryDTO(dto);

    Subcategory subcategory = new Subcategory();
    applySubcategoryDTO(subcategory, dto, true);

    String savedPic = FileUploadUtil.save(pic, "subcategories");
    if (savedPic != null) {
      subcategory.setPic(savedPic);
    }

    return toResponseDTO(subcategoryRepo.save(subcategory));
  }

  public List<SubcategoryResponseDTO> findAll() {
    return subcategoryRepo.findAll()
        .stream()
        .map(this::toResponseDTO)
        .toList();
  }

  public SubcategoryResponseDTO findById(Long id) {
    return toResponseDTO(findEntityById(id));
  }

  public SubcategoryResponseDTO update(Long id, SubcategoryDTO dto, MultipartFile pic) throws IOException {
    validateSubcategoryDTO(dto);

    Subcategory existing = findEntityById(id);
    applySubcategoryDTO(existing, dto, false);

    String savedPic = FileUploadUtil.save(pic, "subcategories");
    if (savedPic != null) {
      existing.setPic(savedPic);
    }

    return toResponseDTO(subcategoryRepo.save(existing));
  }

  public SubcategoryResponseDTO updatePlain(Long id, SubcategoryDTO dto) {
    validateSubcategoryDTO(dto);

    Subcategory existing = findEntityById(id);
    applySubcategoryDTO(existing, dto, false);

    return toResponseDTO(subcategoryRepo.save(existing));
  }

  public void delete(Long id) {
    Subcategory subcategory = findEntityById(id);

    if (productRepo.existsBySubcategory(subcategory.getName())) {
      throw new ConflictException("Cannot delete subcategory because products still use it");
    }

    if (subcategory.getPic() != null) {
      deleteFile(subcategory.getPic());
    }

    subcategoryRepo.deleteById(id);
  }

  private Subcategory findEntityById(Long id) {
    return subcategoryRepo.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Subcategory not found"));
  }

  private void validateSubcategoryDTO(SubcategoryDTO dto) {
    if (dto == null) {
      throw new BadRequestException("Subcategory data is required");
    }

    if (dto.getName() == null || dto.getName().isBlank()) {
      throw new BadRequestException("Subcategory name is required");
    }
  }

  private void applySubcategoryDTO(Subcategory subcategory, SubcategoryDTO dto, boolean creating) {
    subcategory.setName(dto.getName());

    if (dto.getActive() != null) {
      subcategory.setActive(dto.getActive());
    } else if (creating) {
      subcategory.setActive(true);
    }
  }

  private SubcategoryResponseDTO toResponseDTO(Subcategory subcategory) {
    return SubcategoryResponseDTO.builder()
        .id(subcategory.getId())
        .name(subcategory.getName())
        .pic(subcategory.getPic())
        .active(subcategory.isActive())
        .build();
  }

  private void deleteFile(String filePath) {
    try {
      Path path = Path.of(uploadDir, new File(filePath).getName());
      Files.deleteIfExists(path);
    } catch (IOException e) {
      System.err.println("Error deleting file: " + filePath + " - " + e.getMessage());
    }
  }
}