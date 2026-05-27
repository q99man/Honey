package com.honeytong.common.config;

import com.honeytong.common.upload.ImageUploadProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;
    private final ImageUploadProperties imageUploadProperties;

    public WebConfig(
            @Value("${app.cors.allowed-origins}") String[] allowedOrigins,
            ImageUploadProperties imageUploadProperties
    ) {
        this.allowedOrigins = allowedOrigins;
        this.imageUploadProperties = imageUploadProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPath = imageUploadProperties.publicPath().replaceAll("/+$", "");
        registry.addResourceHandler(publicPath + "/**")
                .addResourceLocations(imageUploadProperties.storagePath().toUri().toString());
    }
}
