package com.prashant.api.ecom.ducart.controllers;

import com.prashant.api.ecom.ducart.exception.GlobalExceptionHandler;
import com.prashant.api.ecom.ducart.repositories.ProductRepo;
import com.prashant.api.ecom.ducart.services.CatalogService;
import com.prashant.api.ecom.ducart.services.ProductService;
import com.prashant.api.ecom.ducart.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@Import({CatalogService.class, GlobalExceptionHandler.class, ProductControllerTest.TestSecurityConfig.class})
class CatalogControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductRepo products;
    @MockitoBean ProductService mapper;
    @MockitoBean JwtService jwt;

    @Test
    @SuppressWarnings("unchecked")
    void anonymousSearchReturnsStablePageContract() throws Exception {
        when(products.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 12), 0));
        mvc.perform(get("/product/search").param("q", "blue").param("mc", "Clothes"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(0)).andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void invalidQueryReturns400RatherThanInternalError() throws Exception {
        mvc.perform(get("/product/search").param("size", "all"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid size"));
        verifyNoInteractions(products);
    }
}
