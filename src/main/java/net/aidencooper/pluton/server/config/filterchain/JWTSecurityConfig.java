package net.aidencooper.pluton.server.config.filterchain;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration
@EnableWebSecurity
public class JWTSecurityConfig {
    @Bean
    @Order(2)
    public SecurityFilterChain resourceSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/test/unprotected")
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                ).sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(server -> server.jwt(Customizer.withDefaults()))
                .build();
    }
}
