package net.aidencooper.pluton_server.security.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
@RequestMapping("/api/v1/test")
public class TestProtectedController {
    @GetMapping()
    public ResponseEntity<String> getProtectedEndpoint() {
        return ResponseEntity.ok("Hello Protected World!");
    }
}
