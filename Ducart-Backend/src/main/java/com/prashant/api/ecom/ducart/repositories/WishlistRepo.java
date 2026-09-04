package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepo extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByUser(String user);

    boolean existsByUserAndProduct(String user, String product);
}