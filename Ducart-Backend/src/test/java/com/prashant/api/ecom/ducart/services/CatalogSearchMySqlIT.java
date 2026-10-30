package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.support.MySqlTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogSearchMySqlIT extends CatalogSearchTest {
    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) { MySqlTestDatabase.configure(registry); }
}
