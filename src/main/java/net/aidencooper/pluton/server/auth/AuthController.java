package net.aidencooper.pluton.server.auth;

import lombok.RequiredArgsConstructor;
import net.aidencooper.pluton.server.auth.request.LoginRequest;
import net.aidencooper.pluton.server.auth.request.SignupRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest request) {
        this.authService.login(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody SignupRequest request) {
        this.authService.signup(request);
        return ResponseEntity.ok().build();
    }
}
