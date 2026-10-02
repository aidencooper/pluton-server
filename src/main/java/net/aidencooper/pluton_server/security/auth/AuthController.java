package net.aidencooper.pluton_server.security.auth;

import net.aidencooper.pluton_server.security.email.EmailSenderService;
import net.aidencooper.pluton_server.security.email.EmailVerificationService;
import net.aidencooper.pluton_server.security.email.InvalidVerificationCodeException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.aidencooper.pluton_server.security.jwt.token.AccessTokenService;
import net.aidencooper.pluton_server.security.jwt.token.InvalidRefreshTokenException;
import net.aidencooper.pluton_server.security.jwt.token.RefreshTokenService;
import net.aidencooper.pluton_server.security.user.User;
import net.aidencooper.pluton_server.security.user.UserService;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final EmailSenderService emailSenderService;
    private final EmailVerificationService emailVerificationService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AccessTokenService accessTokenService, RefreshTokenService refreshTokenService, UserService userService, PasswordEncoder passwordEncoder, EmailVerificationService emailVerificationService, EmailSenderService emailSenderService) {
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.emailSenderService = emailSenderService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(Authentication authentication) {
        if(authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) 
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        
        User user = this.userService.loadUserByUsername(authentication.getName());
        if(!user.isEmailVerified())
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "EMAIL_NOT_VERIFIED", "message", "Please verify your email before logging in"));

        String accessToken = this.accessTokenService.generateToken(authentication);
        String refreshToken = this.refreshTokenService.createToken(authentication.getName());

        return ResponseEntity.ok(new TokenResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestParam String refreshToken) {
        try {
            String username = this.refreshTokenService.validateAndRotate(refreshToken);

            User user = this.userService.loadUserByUsername(username);
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
        if(this.userService.userExists(username))
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists: " + username);
        
        User user = User
            .with(email, username)
            .password(this.passwordEncoder.encode(password))
            .roles("USER")
            .build();

        this.userService.createUser(user);

        String code = this.emailVerificationService.generateCode(user.getId());
        this.emailSenderService.sendVerificationCode(user.getEmail(), code);

        return ResponseEntity.status(HttpStatus.CREATED).body("Registered: " + email + " " + username + ". Check your email for a verification code.");
    }
    
    @PostMapping("/email/resend-code")
    public ResponseEntity<?> resendVerificationCode(@RequestParam String email) {
        Map<String, Object> userRow = this.emailVerificationService.findUserByEmail(email);

        if(userRow == null) return ResponseEntity.ok().build();
        if((boolean) userRow.get("email_verified")) return ResponseEntity.ok().build();

        UUID userId = (UUID) userRow.get("id");
        String code = this.emailVerificationService.generateCode(userId);
        this.emailSenderService.sendVerificationCode(email, code);

        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/email/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String email, @RequestParam String code) {
        Map<String, Object> userRow = this.emailVerificationService.findUserByEmail(email);
        if(userRow == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid email or code");

        UUID userId = (UUID) userRow.get("id");
        try {
            this.emailVerificationService.verifyCode(userId, code);
            return ResponseEntity.ok().build();
        } catch (InvalidVerificationCodeException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}