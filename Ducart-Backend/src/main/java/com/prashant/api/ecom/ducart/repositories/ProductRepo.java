package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.Product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ProductRepo extends JpaRepository<Product, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForCheckout(@Param("id") Long id);
    Optional<Product> findByName(String name);

    boolean existsByBrand(String brand);

    boolean existsByMaincategory(String maincategory);

    boolean existsBySubcategory(String subcategory);



}
