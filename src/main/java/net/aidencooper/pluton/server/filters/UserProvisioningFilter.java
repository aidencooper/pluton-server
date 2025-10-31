package net.aidencooper.pluton.server.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.aidencooper.pluton.server.domain.User;
import net.aidencooper.pluton.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;


// A filter to create new users in the database
@Component
public class UserProvisioningFilter extends OncePerRequestFilter {
    private final UserRepository userRepository;

    public UserProvisioningFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt jwt) {
            UUID keycloakUUID = UUID.fromString(jwt.getSubject());

            if(!this.userRepository.existsById(keycloakUUID)) {
                User user = new User();

                user.setId(keycloakUUID);
                user.setUsername(jwt.getClaimAsString("username"));
                user.setEmail(jwt.getClaimAsString("email"));

                this.userRepository.save(user);
            }
        }

        filterChain.doFilter(request, response);
    }
}
