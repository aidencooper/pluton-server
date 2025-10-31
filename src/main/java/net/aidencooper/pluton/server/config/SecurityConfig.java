package net.aidencooper.pluton.server.config;

import net.aidencooper.pluton.server.filters.UserProvisioningFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UserProvisioningFilter userProvisioningFilter) throws Exception {
        // Every request will be authenticated by default
        http.authorizeHttpRequests(authorized -> authorized.anyRequest().authenticated())
                // Disable CSRF because the session management is stateless
                .csrf(AbstractHttpConfigurer::disable)
                // Set session management to a stateless policy
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Set as an OAuth2 JWT Resource Server
                // .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // Only execute the UserProvisioningFilter after the OAuth2 BearerTokenAuthenticationFilter happens
                .addFilterAfter(userProvisioningFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
