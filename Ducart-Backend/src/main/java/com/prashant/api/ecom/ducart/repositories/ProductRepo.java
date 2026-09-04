package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.Product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepo extends JpaRepository<Product, Long> {
    Optional<Product> findByName(String name);

    boolean existsByBrand(String brand);

    boolean existsByMaincategory(String maincategory);

    boolean existsBySubcategory(String subcategory);

   

}
