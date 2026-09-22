package net.aidencooper.pluton_server.security.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.aidencooper.pluton_server.security.jwt.token.AccessTokenService;
import net.aidencooper.pluton_server.security.jwt.token.InvalidRefreshTokenException;
import net.aidencooper.pluton_server.security.jwt.token.RefreshTokenService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.PostMapping;



@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AccessTokenService accessTokenService, RefreshTokenService refreshTokenService, UserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        String accessToken = this.accessTokenService.generateToken(authentication);
        String refreshToken = this.refreshTokenService.createToken(authentication.getName());

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
        try {
            String username = this.refreshTokenService.validateAndRotate(refreshToken);

            UserDetails user = this.userDetailsManager.loadUserByUsername(username);
            Authentication newAuthentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            String newAccessToken = this.accessTokenService.generateToken(newAuthentication);
            String newRefreshToken = this.refreshTokenService.createToken(username);

            return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken));
        } catch(InvalidRefreshTokenException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam String refreshToken) {
        this.refreshTokenService.revoke(refreshToken);
        return ResponseEntity.ok().build();
    }
    
    

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String email, @RequestParam String username, @RequestParam String password) {
        if(userDetailsManager.userExists(username))
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists: " + username);

        UserDetails user = User
            .withUsername(username)
            .password(this.passwordEncoder.encode(password))
            .roles("USER")
            .build();
        
        this.userDetailsManager.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("Registered: " + username);
    }
    
}
