package com.prashant.api.ecom.ducart.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;

public final class MySqlTestDatabase {
    // One isolated database per test JVM; Ryuk removes it when that JVM exits.
    public static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("ducart_integration")
            .withUsername("integration")
            .withPassword("test-only-password");

    static { MYSQL.start(); }

    private MySqlTestDatabase() {}

    public static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }
}
