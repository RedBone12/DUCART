package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.ProductDTO;
import com.prashant.api.ecom.ducart.modal.ProductResponseDTO;
import com.prashant.api.ecom.ducart.modal.ProductStockUpdateDTO;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepo productRepo;

    public ProductService(ProductRepo productRepo) {
        this.productRepo = productRepo;
    }

    public ProductResponseDTO create(ProductDTO productDTO, MultipartFile[] files) throws IOException {
        validateProductDTO(productDTO);
        Product product = new Product();
        applyProductDTO(product, productDTO, true);

        List<String> savedPics = saveProductFiles(files);
        product.setPics(savedPics);

        Product savedProduct = productRepo.save(product);

        return toResponseDTO(savedProduct);
    }

    public List<ProductResponseDTO> findAll() {
        return productRepo.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductResponseDTO findById(Long id) {
        Product product = findProductEntityById(id);

        return toResponseDTO(product);
    }

    public ProductResponseDTO update(Long id, ProductDTO productDTO, MultipartFile[] files) throws IOException {
        validateProductDTO(productDTO);
        Product existing = findProductEntityById(id);

        applyProductDTO(existing, productDTO, false);

        List<String> newPics = saveProductFiles(files);

        if (!newPics.isEmpty()) {
            existing.setPics(newPics);
        }

        Product updatedProduct = productRepo.save(existing);

        return toResponseDTO(updatedProduct);
    }

    public ProductResponseDTO updateStock(Long id, ProductStockUpdateDTO stockDTO) {
        Product existing = findProductEntityById(id);

        if (stockDTO.getStockQuantity() == null) {
            throw new BadRequestException("Stock quantity is required");
        }

        if (stockDTO.getStockQuantity() < 0) {
            throw new BadRequestException("Stock quantity cannot be negative");
        }

        int quantity = stockDTO.getStockQuantity();

        existing.setStockQuantity(quantity);

        boolean requestedStock = stockDTO.getStock() == null
                ? quantity > 0
                : stockDTO.getStock();

        existing.setStock(quantity > 0 && requestedStock);

        Product updatedProduct = productRepo.save(existing);

        return toResponseDTO(updatedProduct);
    }

    public void delete(Long id) {
        if (!productRepo.existsById(id)) {
            throw new ResourceNotFoundException("Product not found");
        }

        productRepo.deleteById(id);
    }

    private Product findProductEntityById(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private void applyProductDTO(Product product, ProductDTO productDTO, boolean creating) {
        product.setName(productDTO.getName());
        product.setMaincategory(productDTO.getMaincategory());
        product.setSubcategory(productDTO.getSubcategory());
        product.setBrand(productDTO.getBrand());
        product.setColor(productDTO.getColor());
        product.setSize(productDTO.getSize());
        product.setBasePrice(productDTO.getBasePrice());
        product.setDiscount(productDTO.getDiscount());
        product.setDescription(productDTO.getDescription());
        product.setStockQuantity(productDTO.getStockQuantity());

        product.setFinalPrice(calculateFinalPrice(
                productDTO.getBasePrice(),
                productDTO.getDiscount()));

        int quantity = productDTO.getStockQuantity();

        boolean requestedStock = productDTO.getStock() == null
                ? quantity > 0
                : productDTO.getStock();

        product.setStock(quantity > 0 && requestedStock);

        if (productDTO.getActive() != null) {
            product.setActive(productDTO.getActive());
        } else if (creating) {
            product.setActive(true);
        }
    }

    private void validateProductDTO(ProductDTO productDTO) {
        if (productDTO == null) {
            throw new BadRequestException("Product data is required");
        }
        if (productDTO.getName() == null || productDTO.getName().isBlank()) {
            throw new BadRequestException("Product name is required");
        }
        if (productDTO.getMaincategory() == null || productDTO.getMaincategory().isBlank()) {
            throw new BadRequestException("Main category is required");
        }
        if (productDTO.getSubcategory() == null || productDTO.getSubcategory().isBlank()) {
            throw new BadRequestException("Subcategory is required");
        }
        if (productDTO.getBrand() == null || productDTO.getBrand().isBlank()) {
            throw new BadRequestException("Brand is required");
        }
        if (productDTO.getColor() == null || productDTO.getColor().isBlank()) {
            throw new BadRequestException("Color is required");
        }
        if (productDTO.getSize() == null || productDTO.getSize().isBlank()) {
            throw new BadRequestException("Size is required");
        }
        if (productDTO.getBasePrice() == null || productDTO.getBasePrice() < 0) {
            throw new BadRequestException("Base price must be 0 or greater");
        }
        if (productDTO.getDiscount() == null || productDTO.getDiscount() < 0 || productDTO.getDiscount() > 100) {
            throw new BadRequestException("Discount must be between 0 and 100");
        }
        if (productDTO.getDescription() == null || productDTO.getDescription().isBlank()) {
            throw new BadRequestException("Description is required");
        }
        if (productDTO.getStockQuantity() == null || productDTO.getStockQuantity() < 0) {
            throw new BadRequestException("Stock quantity cannot be negative");
        }
    }

    private double calculateFinalPrice(double basePrice, double discount) {
        double finalPrice = basePrice - (basePrice * discount / 100);

        return Math.round(finalPrice * 100.0) / 100.0;
    }

    private List<String> saveProductFiles(MultipartFile[] files) throws IOException {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        return FileUploadUtil.saveAll(files, "products");
    }

    private ProductResponseDTO toResponseDTO(Product product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .maincategory(product.getMaincategory())
                .subcategory(product.getSubcategory())
                .brand(product.getBrand())
                .color(product.getColor())
                .size(product.getSize())
                .basePrice(product.getBasePrice())
                .discount(product.getDiscount())
                .finalPrice(product.getFinalPrice())
                .stock(product.isStock())
                .description(product.getDescription())
                .stockQuantity(product.getStockQuantity())
                .pics(product.getPics())
                .active(product.isActive())
                .build();
    }
}
