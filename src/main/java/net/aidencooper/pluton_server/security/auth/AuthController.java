package net.aidencooper.pluton_server.security.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.aidencooper.pluton_server.security.jwt.TokenService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.PostMapping;


@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final TokenService tokenService;
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public AuthController(TokenService tokenService, UserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        this.tokenService = tokenService;
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials"); 
        
        return ResponseEntity.ok(this.tokenService.generateToken(authentication));
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {
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
