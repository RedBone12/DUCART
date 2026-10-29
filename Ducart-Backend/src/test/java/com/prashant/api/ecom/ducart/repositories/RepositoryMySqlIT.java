package com.prashant.api.ecom.ducart.repositories;

import com.prashant.api.ecom.ducart.entities.User;
import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.support.MySqlTestDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.assertThrows;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryMySqlIT extends RepositoryQueriesTest {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        MySqlTestDatabase.configure(registry);
    }

    @Test
    void databaseRejectsDuplicateUsernames() { assertDuplicate("same", "other@example.test", "2222222222"); }

    @Test
    void databaseRejectsDuplicateEmails() { assertDuplicate("other", "same@example.test", "2222222222"); }

    @Test
    void databaseRejectsDuplicatePhones() { assertDuplicate("other", "other@example.test", "1111111111"); }

    @Test
    void pictureCollectionReloadsFromMySqlAfterClearingPersistenceContext() {
        Product product = new Product(); product.setName("Reloaded item");
        product.setPics(java.util.List.of("front.png", "back.png"));
        product = productRepo.saveAndFlush(product);
        Long id = product.getId();
        entityManager.clear();
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("front.png", "back.png"),
                productRepo.findById(id).orElseThrow().getPics());
    }

    private void assertDuplicate(String username, String email, String phone) {
        userRepo.saveAndFlush(user("same", "same@example.test", "1111111111"));
        assertThrows(DataIntegrityViolationException.class, () -> userRepo.saveAndFlush(user(username, email, phone)));
    }

    private User user(String username, String email, String phone) {
        User user = new User();
        user.setName("Integration Buyer"); user.setUsername(username); user.setEmail(email);
        user.setPhone(phone); user.setPassword("encoded-test-value");
        return user;
    }
}
