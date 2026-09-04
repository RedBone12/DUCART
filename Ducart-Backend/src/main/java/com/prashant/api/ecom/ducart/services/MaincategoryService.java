package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Maincategory;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.MainResponseDTO;
import com.prashant.api.ecom.ducart.modal.MaincategoryDTO;
import com.prashant.api.ecom.ducart.repositories.MaincategoryRepo;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class MaincategoryService {

     private final MaincategoryRepo maincategoryRepo;
     private final ProductRepo productRepo;

     private final String uploadDir = FileUploadUtil.getUploadDirFor("maincategories");

     public MaincategoryService(MaincategoryRepo maincategoryRepo, ProductRepo productRepo) {
          this.maincategoryRepo = maincategoryRepo;
          this.productRepo = productRepo;
     }

     public MainResponseDTO create(MaincategoryDTO dto, MultipartFile pic) throws IOException {
          validateMaincategoryDTO(dto);

          Maincategory maincategory = new Maincategory();
          applyMaincategoryDTO(maincategory, dto, true);

          String savedPic = FileUploadUtil.save(pic, "maincategories");
          if (savedPic != null) {
               maincategory.setPic(savedPic);
          }

          return toResponseDTO(maincategoryRepo.save(maincategory));
     }

     public List<MainResponseDTO> findAll() {
          return maincategoryRepo.findAll()
                    .stream()
                    .map(this::toResponseDTO)
                    .toList();
     }

     public MainResponseDTO findById(Long id) {
          return toResponseDTO(findEntityById(id));
     }

     public MainResponseDTO update(Long id, MaincategoryDTO dto, MultipartFile pic) throws IOException {
          validateMaincategoryDTO(dto);

          Maincategory existing = findEntityById(id);
          applyMaincategoryDTO(existing, dto, false);

          String savedPic = FileUploadUtil.save(pic, "maincategories");
          if (savedPic != null) {
               existing.setPic(savedPic);
          }

          return toResponseDTO(maincategoryRepo.save(existing));
     }

     public MainResponseDTO updatePlain(Long id, MaincategoryDTO dto) {
          validateMaincategoryDTO(dto);

          Maincategory existing = findEntityById(id);
          applyMaincategoryDTO(existing, dto, false);

          return toResponseDTO(maincategoryRepo.save(existing));
     }

     public void delete(Long id) {
          Maincategory maincategory = findEntityById(id);

          if (productRepo.existsByMaincategory(maincategory.getName())) {
               throw new ConflictException("Cannot delete main category because products still use it");
          }

          if (maincategory.getPic() != null) {
               deleteFile(maincategory.getPic());
          }

          maincategoryRepo.deleteById(id);
     }

     private Maincategory findEntityById(Long id) {
          return maincategoryRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Main category not found"));
     }

     private void validateMaincategoryDTO(MaincategoryDTO dto) {
          if (dto == null) {
               throw new BadRequestException("Main category data is required");
          }

          if (dto.getName() == null || dto.getName().isBlank()) {
               throw new BadRequestException("Main category name is required");
          }
     }

     private void applyMaincategoryDTO(Maincategory maincategory, MaincategoryDTO dto, boolean creating) {
          maincategory.setName(dto.getName());

          if (dto.getActive() != null) {
               maincategory.setActive(dto.getActive());
          } else if (creating) {
               maincategory.setActive(true);
          }
     }

     private MainResponseDTO toResponseDTO(Maincategory maincategory) {
          return MainResponseDTO.builder()
                    .id(maincategory.getId())
                    .name(maincategory.getName())
                    .pic(maincategory.getPic())
                    .active(maincategory.isActive())
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