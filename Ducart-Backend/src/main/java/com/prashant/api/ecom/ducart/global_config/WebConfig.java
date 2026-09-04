package com.prashant.api.ecom.ducart.global_config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:3000", "http://localhost:3001", "http://127.0.0.1:3000", "http://127.0.0.1:3001")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String currentUploads = Paths.get("uploads")
            .toAbsolutePath()
            .normalize()
            .toUri()
            .toString();

    registry.addResourceHandler("/uploads/**")
            .addResourceLocations(currentUploads);
}
}
