package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.modal.CatalogPage;
import com.prashant.api.ecom.ducart.services.CatalogService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/product/search")
public class CatalogController {
    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) { this.catalog = catalog; }

    @GetMapping
    public CatalogPage search(@RequestParam Map<String, String> query) {
        return catalog.search(query);
    }
}
