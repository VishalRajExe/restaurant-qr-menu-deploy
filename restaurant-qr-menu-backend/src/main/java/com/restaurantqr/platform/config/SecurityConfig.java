package com.restaurantqr.platform.config;

import com.restaurantqr.platform.security.JwtAuthenticationFilter;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the application.
 * Includes CORS, JWT authentication, session management, and security headers.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final RateLimitingFilter rateLimitingFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.admin-url}")
    private String adminUrl;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @PostConstruct
    public void init() {
        log.debug("SecurityConfig initialized");
    }

    // ─── Public endpoints (no auth needed) ────────────────────────────────────
    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/invitations/**",
            "/public/**",
            "/actuator/health",
            "/actuator/info",
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(loggingFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(securityHeadersFilter(), UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            var apiResponse = com.restaurantqr.platform.common.ApiResponse.error("Full authentication is required to access this resource");
                            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                            mapper.writeValue(response.getOutputStream(), apiResponse);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            var apiResponse = com.restaurantqr.platform.common.ApiResponse.error("Access denied: you don't have permission for this action");
                            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                            mapper.writeValue(response.getOutputStream(), apiResponse);
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Customer-facing restaurant lookups & public read queries (used by frontend dashboard & menu pages) — read-only, no auth required.
                        .requestMatchers(HttpMethod.GET, "/restaurants/**").permitAll()
                        // Public pricing/plan comparison
                        .requestMatchers(HttpMethod.GET, "/subscriptions/plans").permitAll()
                        // Super admin only
                        .requestMatchers("/super-admin/**").hasRole("SUPER_ADMIN")
                        // Restaurant owner, manager and staff (mutating actions like POST/PUT/DELETE)
                        .requestMatchers("/restaurants/**").hasAnyRole("RESTAURANT_OWNER", "MANAGER", "STAFF", "SUPER_ADMIN")
                        .requestMatchers("/subscriptions/**").hasAnyRole("RESTAURANT_OWNER", "SUPER_ADMIN")
                        // All authenticated
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .build();
    }

    private Filter loggingFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                log.debug("Request URI: {}", httpRequest.getRequestURI());
                chain.doFilter(request, response);
            }
        };
    }

    /**
     * Security headers filter to protect against common vulnerabilities.
     */
    private Filter securityHeadersFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                // Security Headers
                httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains"); // HSTS
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
                httpResponse.setHeader("X-Frame-Options", "DENY");
                httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
                // Content Security Policy - adjust based on your frontend requirements
                httpResponse.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'");
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");

                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl, adminUrl, "http://localhost:4200", "http://localhost:4201"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Prepends the context path to the given pattern or array of patterns.
     * Handles null or empty context path by returning the original pattern(s).
     * Ensures no double slashes in the resulting pattern.
     */
    private String[] prependContextPath(String[] patterns) {
        if (contextPath == null || contextPath.isEmpty()) {
            return patterns;
        }
        // Remove trailing slash from context path if present
        String ctx = contextPath;
        if (ctx.endsWith("/")) {
            ctx = ctx.substring(0, ctx.length() - 1);
        }
        String[] result = new String[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            String pattern = patterns[i];
            // Remove leading slash from pattern if present
            if (pattern.startsWith("/")) {
                pattern = pattern.substring(1);
            }
            // Ensure no double slash by only adding slash if context path doesn't end with slash
            // and pattern doesn't start with slash (which we already removed)
            result[i] = ctx + "/" + pattern;
        }
        return result;
    }

    /**
     * Overload for single pattern.
     */
    private String prependContextPath(String pattern) {
        return prependContextPath(new String[]{pattern})[0];
    }
}