package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class RepositoryQueriesTest {

    @Autowired UserRepo userRepo;
    @Autowired ProductRepo productRepo;
    @Autowired CartRepo cartRepo;
    @Autowired CheckoutRepo checkoutRepo;
    @Autowired WishlistRepo wishlistRepo;
    @Autowired NewsletterRepo newsletterRepo;

    @Test
    void userQueries_shouldFindByIdentityAndDetectDuplicates() {
        User user = new User();
        user.setName("Alice");
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPhone("1234567890");
        user.setPassword("encoded");
        userRepo.saveAndFlush(user);

        assertEquals(user.getUserid(), userRepo.findByUsername("alice").orElseThrow().getUserid());
        assertEquals(user.getUserid(), userRepo.findByEmail("alice@example.com").orElseThrow().getUserid());
        assertTrue(userRepo.existsByUsername("alice"));
        assertTrue(userRepo.existsByEmail("alice@example.com"));
        assertTrue(userRepo.existsByPhone("1234567890"));
        assertFalse(userRepo.existsByUsername("missing"));
    }

    @Test
    void productQueriesAndPictureMapping_shouldRoundTripElementCollection() {
        Product product = new Product();
        product.setName("Phone");
        product.setMaincategory("Electronics");
        product.setSubcategory("Mobiles");
        product.setBrand("Apple");
        product.setPics(List.of("front.jpg", "back.jpg"));
        product = productRepo.saveAndFlush(product);

        productRepo.flush();
        Product reloaded = productRepo.findById(product.getId()).orElseThrow();

        assertEquals(List.of("front.jpg", "back.jpg"), reloaded.getPics());
        assertEquals(product.getId(), productRepo.findByName("Phone").orElseThrow().getId());
        assertTrue(productRepo.existsByBrand("Apple"));
        assertTrue(productRepo.existsByMaincategory("Electronics"));
        assertTrue(productRepo.existsBySubcategory("Mobiles"));
        assertFalse(productRepo.existsByBrand("Missing"));
    }

    @Test
    void ownerQueries_shouldReturnOnlyRequestedUsersRows() {
        cartRepo.save(cart("alice", "Phone"));
        cartRepo.save(cart("bob", "Laptop"));

        Wishlist aliceWishlist = new Wishlist();
        aliceWishlist.setUser("alice");
        aliceWishlist.setProduct("Phone");
        wishlistRepo.save(aliceWishlist);
        Wishlist bobWishlist = new Wishlist();
        bobWishlist.setUser("bob");
        bobWishlist.setProduct("Laptop");
        wishlistRepo.save(bobWishlist);

        Checkout checkout = new Checkout();
        checkout.setUser("alice");
        checkout.setProductsJson("[{\"product\":\"Phone\"}]");
        checkoutRepo.save(checkout);

        assertEquals(1, cartRepo.findByUser("alice").size());
        assertEquals("Phone", cartRepo.findByUserAndProduct("alice", "Phone").orElseThrow().getProduct());
        assertTrue(cartRepo.findByUserAndProduct("alice", "Laptop").isEmpty());
        assertEquals(1, wishlistRepo.findByUser("alice").size());
        assertTrue(wishlistRepo.existsByUserAndProduct("alice", "Phone"));
        assertFalse(wishlistRepo.existsByUserAndProduct("alice", "Laptop"));
        assertEquals("[{\"product\":\"Phone\"}]", checkoutRepo.findByUser("alice").get(0).getProductsJson());
    }

    @Test
    void newsletterQueries_shouldFindAndDetectExistingEmail() {
        Newsletter newsletter = new Newsletter();
        newsletter.setEmail("news@example.com");
        newsletterRepo.saveAndFlush(newsletter);

        assertTrue(newsletterRepo.findByEmail("news@example.com").isPresent());
        assertTrue(newsletterRepo.existsByEmail("news@example.com"));
        assertFalse(newsletterRepo.existsByEmail("missing@example.com"));
    }

    private Cart cart(String user, String product) {
        Cart cart = new Cart();
        cart.setUser(user);
        cart.setProduct(product);
        return cart;
    }
}
