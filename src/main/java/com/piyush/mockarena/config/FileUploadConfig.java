package com.piyush.mockarena.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${mockarena.file.upload-dir:./uploads/}")
    private String uploadDir;

    @Value("${mockarena.file.profile-pictures-dir:./uploads/profiles/}")
    private String profilePicturesDir;

    @PostConstruct
    public void init() {
        try {
            // Create upload directories if they don't exist
            Path uploadPath = Paths.get(uploadDir);
            Path profilePath = Paths.get(profilePicturesDir);

            Files.createDirectories(uploadPath);
            Files.createDirectories(profilePath);

            log.info("Upload directories created: {} and {}", uploadPath, profilePath);
        } catch (IOException e) {
            log.error("Failed to create upload directories", e);
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }
}
