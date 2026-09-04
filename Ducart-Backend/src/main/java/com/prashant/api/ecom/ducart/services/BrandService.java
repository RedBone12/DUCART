package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Brand;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.BrandDTO;
import com.prashant.api.ecom.ducart.modal.BrandResponseDTO;
import com.prashant.api.ecom.ducart.repositories.BrandRepo;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class BrandService {

    private final BrandRepo brandRepo;
    private final ProductRepo productRepo;

    public BrandService(BrandRepo brandRepo, ProductRepo productRepo) {
        this.brandRepo = brandRepo;
        this.productRepo = productRepo;
    }

    public BrandResponseDTO create(BrandDTO dto, MultipartFile pic) throws IOException {
        validateBrandDTO(dto);

        Brand brand = new Brand();
        applyBrandDTO(brand, dto, true);

        String savedPic = FileUploadUtil.save(pic, "brands");
        if (savedPic != null) {
            brand.setPic(savedPic);
        }

        return toResponseDTO(brandRepo.save(brand));
    }

    public List<BrandResponseDTO> findAll() {
        return brandRepo.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public BrandResponseDTO findById(Long id) {
        return toResponseDTO(findEntityById(id));
    }

    public BrandResponseDTO update(Long id, BrandDTO dto, MultipartFile pic) throws IOException {
        validateBrandDTO(dto);

        Brand existing = findEntityById(id);
        applyBrandDTO(existing, dto, false);

        String savedPic = FileUploadUtil.save(pic, "brands");
        if (savedPic != null) {
            existing.setPic(savedPic);
        }

        return toResponseDTO(brandRepo.save(existing));
    }

    public BrandResponseDTO updatePlain(Long id, BrandDTO dto) {
        validateBrandDTO(dto);

        Brand existing = findEntityById(id);
        applyBrandDTO(existing, dto, false);

        return toResponseDTO(brandRepo.save(existing));
    }

    public void delete(Long id) {
        Brand brand = findEntityById(id);

        if (productRepo.existsByBrand(brand.getName())) {
            throw new ConflictException("Cannot delete brand because products still use it");
        }

        brandRepo.deleteById(id);
    }

    private Brand findEntityById(Long id) {
        return brandRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
    }

    private void validateBrandDTO(BrandDTO dto) {
        if (dto == null) {
            throw new BadRequestException("Brand data is required");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new BadRequestException("Brand name is required");
        }
    }

    private void applyBrandDTO(Brand brand, BrandDTO dto, boolean creating) {
        brand.setName(dto.getName());

        if (dto.getActive() != null) {
            brand.setActive(dto.getActive());
        } else if (creating) {
            brand.setActive(true);
        }
    }

    private BrandResponseDTO toResponseDTO(Brand brand) {
        return BrandResponseDTO.builder()
                .id(brand.getId())
                .name(brand.getName())
                .pic(brand.getPic())
                .active(brand.isActive())
                .build();
    }
}