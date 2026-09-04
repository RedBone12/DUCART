package com.prashant.api.ecom.ducart.global_config;

import com.prashant.api.ecom.ducart.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WebConfigTest.TestController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import(WebConfig.class)
class WebConfigTest {

    @Autowired MockMvc mockMvc;
    @Autowired ApplicationContext applicationContext;
    @MockitoBean JwtService jwtService;

    @Test
    void cors_shouldAllowConfiguredFrontendOriginAndMethods() throws Exception {
        mockMvc.perform(options("/anything")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PUT"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("PUT")));
    }

    @Test
    void resourceHandler_shouldMapPublicUploadsPath() {
        boolean mapped = applicationContext.getBeansOfType(SimpleUrlHandlerMapping.class).values().stream()
                .anyMatch(mapping -> mapping.getUrlMap().containsKey("/uploads/**"));

        assertTrue(mapped);
    }

    @RestController
    static class TestController {
        @GetMapping("/anything")
        String anything() {
            return "ok";
        }
    }
}
