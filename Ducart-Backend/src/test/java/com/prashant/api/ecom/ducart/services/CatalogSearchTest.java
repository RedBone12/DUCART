package com.prashant.api.ecom.ducart.services;

import com.prashant.api.ecom.ducart.entities.Product;
import com.prashant.api.ecom.ducart.exception.BadRequestException;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({CatalogService.class, ProductService.class})
class CatalogSearchTest {
    @Autowired CatalogService catalog;
    @Autowired ProductRepo products;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;

    @Test
    void combinesEveryFilterBeforeCountingAndPaging() {
        var first = item("Blue Shoe", "Shoes", "Acme", 20, true);
        var second = item("Blue Shoe", "Shoes", "Acme", 20, true);
        item("Blue Shoe", "Shoes", "Other", 20, true);
        item("Blue Shoe", "Hats", "Acme", 20, true);
        item("Red Shoe", "Shoes", "Acme", 20, true);
        item("Blue Shoe", "Shoes", "Acme", 60, true);
        item("Blue Shoe", "Shoes", "Acme", 20, false);
        var otherCategory = item("Blue Shoe", "Shoes", "Acme", 20, true);
        otherCategory.setMaincategory("Other");
        products.flush(); entityManager.clear();
        var query = new java.util.HashMap<>(Map.of("q", "BLUE", "mc", "Clothes", "sc", "Shoes",
                "br", "Acme", "min", "20.00", "max", "20.00", "sort", "priceAsc", "size", "1"));
        var page = catalog.search(query);
        assertEquals(2, page.totalElements()); assertEquals(2, page.totalPages());
        assertEquals(List.of("front.png"), page.content().get(0).getPics());
        assertEquals(List.of(first.getId()), page.content().stream().map(p -> p.getId()).toList());
        query.put("page", "1");
        assertEquals(second.getId(), catalog.search(query).content().get(0).getId());
        query.put("page", "2");
        assertTrue(catalog.search(query).content().isEmpty());
    }

    @Test
    void searchesAllFieldsLiterallyAndOrdersAcrossPages() {
        var low = item("100%_! Cotton", "Shoes", "Acme", 10, true);
        var high = item("Winter", "Shoes", "Acme", 30, true);
        high.setDescription("x".repeat(500) + " Warm LINING"); high.setColor("Purple"); products.flush();
        assertEquals(low.getId(), catalog.search(Map.of("q", "%_!")).content().get(0).getId());
        for (String q : List.of("lining", "purple", "winter", "clothes", "shoes", "acme")) {
            assertFalse(catalog.search(Map.of("q", q)).content().isEmpty(), q);
        }
        assertEquals(high.getId(), catalog.search(Map.of("sort", "priceDesc", "size", "1")).content().get(0).getId());
        assertEquals(low.getId(), catalog.search(Map.of("sort", "priceDesc", "size", "1", "page", "1")).content().get(0).getId());
        assertEquals(high.getId(), catalog.search(Map.of()).content().get(0).getId());
        assertEquals(1, catalog.search(Map.of("min", "30")).totalElements());
        assertEquals(1, catalog.search(Map.of("max", "10")).totalElements());
        assertEquals(0, catalog.search(Map.of("q", "missing")).totalElements());
        assertEquals(2, catalog.search(Map.of("mc", "All", "q", " ")).totalElements());
    }

    @Test
    void rejectsUnboundedOrMalformedRequestsBeforeQuerying() {
        for (var query : List.of(Map.of("page", "-1"), Map.of("page", "100001"), Map.of("page", "x"),
                Map.of("size", "0"), Map.of("size", "101"), Map.of("sort", "name;drop"),
                Map.of("min", "-1"), Map.of("max", "NaN"), Map.of("min", "1.001"),
                Map.of("min", "2", "max", "1"), Map.of("q", "x".repeat(201)))) {
            assertThrows(BadRequestException.class, () -> catalog.search(query), query.toString());
        }
    }

    private Product item(String name, String subcategory, String brand, double price, boolean active) {
        Product p = new Product(); p.setName(name); p.setMaincategory("Clothes");
        p.setSubcategory(subcategory); p.setBrand(brand); p.setFinalPrice(price);
        p.setActive(active); p.setPics(new java.util.ArrayList<>(List.of("front.png")));
        return products.saveAndFlush(p);
    }
}
