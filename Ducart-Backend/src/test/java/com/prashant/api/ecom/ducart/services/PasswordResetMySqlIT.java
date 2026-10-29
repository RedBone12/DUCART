package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.support.MySqlTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class PasswordResetMySqlIT extends PasswordResetIntegrationTest {
    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        MySqlTestDatabase.configure(registry);
    }
}
