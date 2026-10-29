package com.prashant.api.ecom.ducart.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.cors.allowed-origins=https://shop.example.test")
@AutoConfigureMockMvc
class ProductionCorsTest {
    @Autowired MockMvc mvc;

    @Test
    void explicitProductionOriginIsAllowedForCrossOriginRequests() throws Exception {
        mvc.perform(options("/user/login").header("Origin", "https://shop.example.test")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://shop.example.test"));
    }

    @Test
    void developmentOriginsAreNotImplicitlyAllowedInProduction() throws Exception {
        mvc.perform(options("/user/login").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
