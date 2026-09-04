package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.Newsletter;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsletterRepo extends JpaRepository<Newsletter, Long> {
      // Used to check whether an email has already subscribed.
    Optional<Newsletter> findByEmail(String email);

    // Used for duplicate email validation.
    boolean existsByEmail(String email);
}
