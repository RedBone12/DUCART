package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.Checkout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutRepo extends JpaRepository<Checkout, Long> {
    java.util.List<Checkout> findByUser(String user);
}
