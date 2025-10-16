//// src/main/java/com/piyush/mockarena/config/CorsConfig.java
//package com.piyush.mockarena.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.CorsConfigurationSource;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // Allow your Vite frontend
//        configuration.addAllowedOrigin("http://localhost:5173");
//        configuration.addAllowedOrigin("http://127.0.0.1:5173");
//        configuration.addAllowedOrigin("http://localhost:3000");  // Also support CRA
//
//        // Allow all headers
//        configuration.addAllowedHeader("*");
//
//        // Allow all HTTP methods
//        configuration.addAllowedMethod("GET");
//        configuration.addAllowedMethod("POST");
//        configuration.addAllowedMethod("PUT");
//        configuration.addAllowedMethod("DELETE");
//        configuration.addAllowedMethod("OPTIONS");
//
//        // Allow credentials (important for JWT)
//        configuration.setAllowCredentials(true);
//
//        // Apply to all API endpoints
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/api/**", configuration);
//
//        return source;
//    }
//}
