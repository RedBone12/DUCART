package com.prashant.api.ecom.ducart.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.repositories.*;
import com.prashant.api.ecom.ducart.support.MySqlTestDatabase;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutApiMySqlIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ProductRepo products;
    @Autowired UserRepo users;
    @Autowired CartRepo carts;
    @Autowired CheckoutRepo orders;
    Product product;

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) { MySqlTestDatabase.configure(registry); }

    @BeforeEach
    void setup() {
        orders.deleteAll(); carts.deleteAll(); products.deleteAll(); users.deleteAll();
        product = new Product(); product.setName("API purchase item"); product.setBasePrice(24.99);
        product.setDiscount(0.0); product.setFinalPrice(24.99); product.setActive(true);
        product.setStock(true); product.setStockQuantity(3);
        product = products.saveAndFlush(product);
    }

    @Test
    void realRegistrationJwtCartCheckoutRetryAndOrderIsolation() throws Exception {
        String token = registerAndLogin("api-buyer", "1111111111");
        mvc.perform(post("/cart").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":" + product.getId() + ",\"qty\":2}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/cart/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].qty").value(2));
        String body = "{\"idempotencyKey\":\"api-purchase-request\",\"paymentMode\":\"COD\",\"products\":[{\"productId\":"
                + product.getId() + ",\"qty\":2}]}";
        String saved = mvc.perform(post("/checkout").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(54.97))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(saved).get("id").asLong();
        mvc.perform(post("/checkout").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/checkout/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].products[0].product").value("API purchase item"));
        String other = registerAndLogin("api-other", "2222222222");
        mvc.perform(get("/checkout/me").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertEquals(1, orders.count());
        assertEquals(1, products.findById(product.getId()).orElseThrow().getStockQuantity());
    }

    private String registerAndLogin(String username, String phone) throws Exception {
        mvc.perform(post("/user").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"API Buyer","username":"%s","email":"%s@example.test","phone":"%s","password":"ApiPassword123!"}
                """.formatted(username, username, phone)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.password").doesNotExist());
        String response = mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"ApiPassword123!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("token").asText();
    }
}
