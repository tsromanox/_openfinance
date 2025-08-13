package br.com.openfinance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Security configuration for OpenFinance Receptor.
 * Provides basic security settings with focus on API access and monitoring endpoints.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    /**
     * Configure security filter chain for development environment.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "development", matchIfMissing = true)
    public SecurityFilterChain developmentSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring development security filter chain (permissive)");
        
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/v1/resources/health").permitAll()
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            )
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            );
        
        return http.build();
    }
    
    /**
     * Configure security filter chain for production environment.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "production")
    public SecurityFilterChain productionSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring production security filter chain (restrictive)");
        
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public health and info endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/resources/health").permitAll()
                
                // Protected monitoring endpoints
                .requestMatchers("/actuator/prometheus", "/actuator/metrics").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // API documentation (consider removing in production)
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").hasRole("ADMIN")
                
                // Protected API endpoints
                .requestMatchers("/api/v1/resources/discover", "/api/v1/resources/sync").hasRole("ADMIN")
                .requestMatchers("/api/v1/resources/validate", "/api/v1/resources/monitor").hasRole("ADMIN")
                .requestMatchers("/api/v1/resources/process/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/resources/metrics/**").hasRole("ADMIN")
                
                // Read-only resource endpoints
                .requestMatchers("/api/v1/resources/*/status").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/resources/search").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/resources/*").hasAnyRole("USER", "ADMIN")
                
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {})
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                )
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
            );
        
        return http.build();
    }
}