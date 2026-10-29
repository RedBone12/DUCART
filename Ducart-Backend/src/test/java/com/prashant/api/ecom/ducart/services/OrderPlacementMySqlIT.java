package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.support.MySqlTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class OrderPlacementMySqlIT extends OrderPlacementIntegrationTest {
    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        MySqlTestDatabase.configure(registry);
    }
}
