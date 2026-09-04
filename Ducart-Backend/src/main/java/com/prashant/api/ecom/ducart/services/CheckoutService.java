package com.prashant.api.ecom.ducart.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.*;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.CheckoutDTO;
import com.prashant.api.ecom.ducart.repositories.CheckoutRepo;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CheckoutService {
    private final CheckoutRepo checkoutRepo;
    private final ObjectMapper mapper;

    public CheckoutService(CheckoutRepo checkoutRepo) {
        this.checkoutRepo = checkoutRepo;
        this.mapper = new ObjectMapper();
    }

    public CheckoutDTO create(CheckoutDTO dto) throws Exception {
        if (dto.getDate() == null)
            dto.setDate(java.time.LocalDateTime.now().toString());
        Checkout saved = checkoutRepo.save(toEntity(dto, new Checkout()));
        return toDTO(saved);
    }

    public List<CheckoutDTO> findAll() {
        return checkoutRepo.findAll().stream().map(this::safeToDTO).toList();
    }

    public List<CheckoutDTO> findByUser(String user) {
        return checkoutRepo.findByUser(user).stream().map(this::safeToDTO).toList();
    }

    public CheckoutDTO findById(Long id) {
        return toDTO(checkoutRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found")));
    }

    public CheckoutDTO update(Long id, CheckoutDTO dto) throws Exception {
        Checkout existing = checkoutRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Checkout saved = checkoutRepo.save(toEntity(dto, existing));
        return toDTO(saved);
    }

    public void delete(Long id) {
        if (!checkoutRepo.existsById(id))
            throw new ResourceNotFoundException("Order not found");
        checkoutRepo.deleteById(id);
    }

    private Checkout toEntity(CheckoutDTO dto, Checkout c) throws Exception {
        c.setUser(dto.getUser());
        c.setOrderStatus(dto.getOrderStatus());
        c.setPaymentMode(dto.getPaymentMode());
        c.setPaymentStatus(dto.getPaymentStatus());
        c.setSubtotal(dto.getSubtotal());
        c.setShipping(dto.getShipping());
        c.setTotal(dto.getTotal());
        c.setDate(dto.getDate() == null ? java.time.LocalDateTime.now().toString() : dto.getDate());
        c.setRppid(dto.getRppid());
        c.setProductsJson(mapper.writeValueAsString(Optional.ofNullable(dto.getProducts()).orElse(new ArrayList<>())));
        return c;
    }

    private CheckoutDTO safeToDTO(Checkout c) {
        try {
            return toDTO(c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CheckoutDTO toDTO(Checkout c) {
        List<Cart> products = new ArrayList<>();
        try {
            if (c.getProductsJson() != null && !c.getProductsJson().isBlank()) {
                products = mapper.readValue(c.getProductsJson(), new TypeReference<List<Cart>>() {
                });
            }
        } catch (Exception ignored) {
        }
        return new CheckoutDTO(c.getId(), c.getUser(), c.getOrderStatus(), c.getPaymentMode(), c.getPaymentStatus(),
                c.getSubtotal(), c.getShipping(), c.getTotal(), c.getDate(), c.getRppid(), products);
    }
}
