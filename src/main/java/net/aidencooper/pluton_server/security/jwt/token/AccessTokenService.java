package net.aidencooper.pluton_server.security.jwt.token;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service 
public class AccessTokenService {
    private final JwtEncoder jwtEncoder;

    public AccessTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(Authentication authentication) {
        Instant now = Instant.now();
       
        // Combines all authorities of an authenticated principal and combines them
        // EX: authorities: ROLE_ADMIN SCOPE_READ SCOPE_WRITE
        String scope = authentication.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .collect(Collectors.joining(" "));
        
        // Build the JWT
        // Issuer: Self because there is no other auth server used
        // Subject: Who the token is for
        // Claim: Add the authorities authorized
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject(authentication.getName())
            .claim("scope", scope)
            .build();
        
        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
