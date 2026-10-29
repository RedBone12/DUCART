package com.prashant.api.ecom.ducart.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.*;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.modal.CheckoutDTO;
import com.prashant.api.ecom.ducart.modal.CheckoutLineDTO;
import com.prashant.api.ecom.ducart.repositories.CheckoutRepo;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.repositories.UserRepo;
import com.prashant.api.ecom.ducart.modal.PlaceOrderRequest;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CheckoutService {
    private final CheckoutRepo checkoutRepo;
    private final ObjectMapper mapper;
    private final ProductRepo productRepo;
    private final UserRepo userRepo;

    public CheckoutService(CheckoutRepo checkoutRepo, ProductRepo productRepo, UserRepo userRepo) {
        this.checkoutRepo = checkoutRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.mapper = new ObjectMapper();
    }

    @Transactional(rollbackFor = Exception.class)
    public CheckoutDTO placeOrder(String username, PlaceOrderRequest request) throws Exception {
        if (request == null || request.idempotencyKey() == null
                || !request.idempotencyKey().matches("[A-Za-z0-9-]{8,64}"))
            throw new BadRequestException("A valid idempotency key is required");
        if (request.paymentMode() == null || !Set.of("COD", "Card Demo", "PayPal Demo", "Bank Transfer")
                .contains(request.paymentMode()))
            throw new BadRequestException("Unsupported payment mode");
        if (request.products() == null || request.products().isEmpty() || request.products().size() > 100)
            throw new BadRequestException("Between 1 and 100 products are required");

        // A stable order prevents deadlocks when baskets contain the same products.
        SortedMap<Long, Integer> quantities = new TreeMap<>();
        for (PlaceOrderRequest.Item item : request.products()) {
            if (item == null || item.productId() == null || item.productId() <= 0
                    || item.qty() == null || item.qty() <= 0)
                throw new BadRequestException("Valid product IDs and positive quantities are required");
            long quantity = (long) quantities.getOrDefault(item.productId(), 0) + item.qty();
            if (quantity > Integer.MAX_VALUE) throw new BadRequestException("Quantity is too large");
            quantities.put(item.productId(), (int) quantity);
        }
        String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest((request.paymentMode() + ":" + quantities).getBytes(StandardCharsets.UTF_8)));

        // Serialize retries for this buyer across application instances, including first requests.
        userRepo.findForCheckout(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Optional<Checkout> previous = checkoutRepo.findByUserAndIdempotencyKey(username, request.idempotencyKey());
        if (previous.isPresent()) {
            if (!fingerprint.equals(previous.get().getRequestFingerprint()))
                throw new ConflictException("Idempotency key was already used for a different order");
            return toDTO(previous.get());
        }

        List<CheckoutLineDTO> snapshots = new ArrayList<>();
        BigDecimal subtotal = new BigDecimal("0.00");
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepo.findForCheckout(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + entry.getKey()));
            int quantity = entry.getValue();
            if (!product.isActive() || !product.isStock() || product.getStockQuantity() == null
                    || product.getStockQuantity() < quantity)
                throw new ConflictException("Product unavailable or insufficient stock: " + product.getName());
            if (product.getBasePrice() == null || !Double.isFinite(product.getBasePrice())
                    || product.getBasePrice() < 0 || product.getDiscount() == null
                    || !Double.isFinite(product.getDiscount()) || product.getDiscount() < 0
                    || product.getDiscount() > 100)
                throw new ConflictException("Product pricing is invalid: " + product.getName());
            BigDecimal price = BigDecimal.valueOf(product.getBasePrice())
                    .multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(product.getDiscount()).movePointLeft(2)))
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);
            if (subtotal.precision() > 19) throw new BadRequestException("Order total is too large");
            CheckoutLineDTO snapshot = new CheckoutLineDTO();
            snapshot.setId(product.getId());
            snapshot.setUser(username);
            snapshot.setProduct(product.getName());
            snapshot.setName(product.getName());
            snapshot.setBrand(product.getBrand());
            snapshot.setColor(product.getColor());
            snapshot.setSize(product.getSize());
            snapshot.setQty(quantity);
            snapshot.setPrice(price);
            snapshot.setTotal(lineTotal);
            snapshot.setStockQuantity(product.getStockQuantity());
            if (product.getPics() != null && !product.getPics().isEmpty()) snapshot.setPic(product.getPics().get(0));
            snapshots.add(snapshot);
            product.setStockQuantity(product.getStockQuantity() - quantity);
            product.setStock(product.getStockQuantity() > 0);
            productRepo.save(product);
        }
        BigDecimal shipping = subtotal.signum() > 0 && subtotal.compareTo(new BigDecimal("100.00")) < 0
                ? new BigDecimal("4.99") : new BigDecimal("0.00");
        Checkout order = new Checkout();
        order.setUser(username);
        order.setIdempotencyKey(request.idempotencyKey());
        order.setRequestFingerprint(fingerprint);
        order.setOrderStatus("Order is Placed");
        order.setPaymentMode(request.paymentMode());
        order.setPaymentStatus("Pending");
        order.setDate(java.time.LocalDateTime.now().toString());
        order.setSubtotal(subtotal);
        order.setShipping(shipping);
        order.setTotal(subtotal.add(shipping));
        order.setProductsJson(mapper.writeValueAsString(snapshots));
        return toDTO(checkoutRepo.saveAndFlush(order));
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
        List<CheckoutLineDTO> products = new ArrayList<>();
        try {
            if (c.getProductsJson() != null && !c.getProductsJson().isBlank()) {
                products = mapper.readValue(c.getProductsJson(), new TypeReference<List<CheckoutLineDTO>>() {
                });
            }
        } catch (Exception ignored) {
        }
        return new CheckoutDTO(c.getId(), c.getUser(), c.getOrderStatus(), c.getPaymentMode(), c.getPaymentStatus(),
                c.getSubtotal(), c.getShipping(), c.getTotal(), c.getDate(), c.getRppid(), products);
    }
}
