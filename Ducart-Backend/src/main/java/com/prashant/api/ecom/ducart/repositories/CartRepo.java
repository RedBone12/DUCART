package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.Cart;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepo extends JpaRepository<Cart, Long> {
    java.util.List<Cart> findByUser(String user);

    Optional<Cart> findByUserAndProduct(String user, String product);
}
