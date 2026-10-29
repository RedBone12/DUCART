package com.prashant.api.ecom.ducart.support;

import com.prashant.api.ecom.ducart.DucartBackendApplication;
import com.prashant.api.ecom.ducart.entities.*;
import com.prashant.api.ecom.ducart.repositories.*;
import java.nio.file.*;
import java.util.*;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Test-classpath-only bootstrap; never packaged in the production application. */
public final class BrowserTestServer {
    private BrowserTestServer() {}

    public static void main(String[] args) throws Exception {
        var mysql = MySqlTestDatabase.MYSQL;
        Path uploads = Files.createTempDirectory("ducart-browser-uploads-");
        System.setProperty("ducart.upload-dir", uploads.toString());
        System.setProperty("spring.datasource.url", mysql.getJdbcUrl());
        System.setProperty("spring.datasource.username", mysql.getUsername());
        System.setProperty("spring.datasource.password", mysql.getPassword());
        System.setProperty("spring.datasource.driver-class-name", mysql.getDriverClassName());
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("server.address", "127.0.0.1");
        System.setProperty("server.port", "18080");
        System.setProperty("management.health.mail.enabled", "false");
        ConfigurableApplicationContext app = SpringApplication.run(DucartBackendApplication.class);
        seed(app);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (var files = Files.walk(uploads)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            } catch (java.io.IOException ex) {
                System.err.println("Temporary browser upload cleanup failed");
            }
        }));
        System.out.println("DUCART_BROWSER_FIXTURE_READY");
    }

    private static void seed(ConfigurableApplicationContext app) {
        User admin = new User();
        admin.setName("Browser Admin"); admin.setUsername("e2e-admin"); admin.setEmail("admin@example.test");
        admin.setPhone("1111111111"); admin.setRole("Admin");
        admin.setPassword(app.getBean(PasswordEncoder.class).encode("BrowserAdmin123!"));
        app.getBean(UserRepo.class).saveAndFlush(admin);
        Maincategory main = new Maincategory(); main.setName("BrowserCategory"); main.setActive(true);
        app.getBean(MaincategoryRepo.class).saveAndFlush(main);
        Subcategory sub = new Subcategory(); sub.setName("BrowserSubcategory"); sub.setActive(true);
        app.getBean(SubcategoryRepo.class).saveAndFlush(sub);
        Brand brand = new Brand(); brand.setName("BrowserBrand"); brand.setActive(true);
        app.getBean(BrandRepo.class).saveAndFlush(brand);
        Product product = new Product();
        product.setName("Browser Seed Product"); product.setMaincategory(main.getName());
        product.setSubcategory(sub.getName()); product.setBrand(brand.getName());
        product.setColor("Blue"); product.setSize("Medium"); product.setDescription("Browser test catalogue item");
        product.setBasePrice(24.99); product.setDiscount(0.0); product.setFinalPrice(24.99);
        product.setStockQuantity(10); product.setStock(true); product.setActive(true);
        app.getBean(ProductRepo.class).saveAndFlush(product);
    }
}
