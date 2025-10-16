package com.piyush.mockarena.config;

import com.piyush.mockarena.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow specific origins (your React frontend)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",    // Create React App
                "http://localhost:5173",    // Vite
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "http://localhost:4200"     // Angular
        ));

        // Allow all headers
        configuration.setAllowedHeaders(List.of("*"));

        // Allow all HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Allow credentials (for JWT tokens)
        configuration.setAllowCredentials(true);

        // How long the browser can cache preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF for API
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless session management
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // ========================================
                        // PUBLIC ENDPOINTS (No authentication needed)
                        // ========================================

                        // Authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Problem-related public endpoints
                        .requestMatchers(HttpMethod.GET, "/api/problems").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/problems/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/problems/*/template").permitAll()  // ← Fixed template endpoint

                        // Language and tag endpoints
                        .requestMatchers("/api/languages/**").permitAll()
                        .requestMatchers("/api/tags/**").permitAll()

                        // Public contest endpoints
                        .requestMatchers(HttpMethod.GET, "/api/contests").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/contests/*").permitAll()
                        .requestMatchers("/api/contests/public/**").permitAll()

                        // Leaderboard endpoints
                        .requestMatchers("/api/leaderboard/**").permitAll()

                        // Health check and test endpoints
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/users/test").permitAll()

                        // Development and documentation tools
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                        // ========================================
                        // PROTECTED ENDPOINTS (Authentication required)
                        // ========================================

                        // User submissions and code execution
                        .requestMatchers("/api/submissions/**").authenticated()
                        .requestMatchers("/api/run/**").authenticated()

                        // User management and profiles
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/profile/**").authenticated()

                        // Contest participation
                        .requestMatchers(HttpMethod.POST, "/api/contests/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/contests/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/contests/**").authenticated()

                        // Problem creation/modification (Admin only)
                        .requestMatchers(HttpMethod.POST, "/api/problems").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/problems/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/problems/**").hasRole("ADMIN")

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Add JWT authentication
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // ✅ FIXED: Simplified headers configuration that works with all Spring Security versions
        http.headers(headers -> headers
                .frameOptions().sameOrigin()  // For H2 Console
        );

        return http.build();
    }
}
