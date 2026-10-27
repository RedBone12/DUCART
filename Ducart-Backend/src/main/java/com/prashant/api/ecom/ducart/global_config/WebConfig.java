package com.prashant.api.ecom.ducart.global_config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.*;
import com.prashant.api.ecom.ducart.utils.FileUploadUtil;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001,http://127.0.0.1:3000,http://127.0.0.1:3001}")
    private String[] allowedOrigins;
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }

    @Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String currentUploads = FileUploadUtil.uploadRoot()
            .toUri()
            .toString();

    registry.addResourceHandler("/uploads/**")
            .addResourceLocations(currentUploads);
}
}
