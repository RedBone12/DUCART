package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.entities.Wishlist;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.exception.ConflictException;
import com.prashant.api.ecom.ducart.exception.ResourceNotFoundException;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.repositories.WishlistRepo;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class WishlistService {

    private final WishlistRepo wishlistRepo;
    private final ProductRepo productRepo;

    public WishlistService(WishlistRepo wishlistRepo, ProductRepo productRepo) {
        this.wishlistRepo = wishlistRepo;
        this.productRepo = productRepo;
    }

    public List<Wishlist> getAllWishlistItems() {
        return wishlistRepo.findAll();
    }

    public List<Wishlist> getWishlistItemsByUser(String username) {
        return wishlistRepo.findByUser(username);
    }

    public Wishlist createWishlistItem(Wishlist item, String username) {
        Product product = requireProduct(item);

        boolean alreadyExists = wishlistRepo.existsByUserAndProduct(username, product.getName());

        if (alreadyExists) {
            throw new ConflictException("Product already exists in wishlist");
        }

        /*
         * 不允许前端指定 wishlist id。
         * 创建新收藏记录时，id 应该由数据库自动生成。
         */
        item.setId(null);

        /*
         * 不相信前端传来的 user。
         * user 必须使用 JWT 中当前登录用户的 username。
         */
        item.setUser(username);

        /*
         * 商品名称、价格、库存、图片等信息从数据库里的 Product 获取，
         * 而不是直接相信前端发送的数据。
         */
        applyProductDetails(item, product);

        return wishlistRepo.save(item);
    }

    public void deleteWishlistItem(Long id, String username, boolean admin) {
        Wishlist existing = wishlistRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException( "Wishlist item not found"));

        checkPermission(existing, username, admin);

        wishlistRepo.deleteById(id);
    }

    private Product requireProduct(Wishlist item) {
        if (item.getProduct() == null || item.getProduct().isBlank()) {
            throw new BadRequestException("Product is required");
        }

        return productRepo.findByName(item.getProduct())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found"));
    }

    private void checkPermission(Wishlist item, String username, boolean admin) {

        boolean owner = Objects.equals(item.getUser(), username);

        if (!admin && !owner) {
            throw new AccessDeniedException("You do not have permission to delete this wishlist item");
        }
    }

    private void applyProductDetails(Wishlist item, Product product) {

        Double price = Optional
                .ofNullable(product.getFinalPrice())
                .orElse(Optional
                                .ofNullable(product.getBasePrice())
                                .orElse(0.0));

        item.setProduct(product.getName());
        item.setName(product.getName());
        item.setBrand(product.getBrand());
        item.setColor(product.getColor());
        item.setSize(product.getSize());
        item.setPrice(price);
        item.setStockQuantity(product.getStockQuantity());

        if (product.getPics() != null && !product.getPics().isEmpty()) {
            item.setPic(product.getPics().get(0));
        } else {
            item.setPic(null);
        }
    }
}