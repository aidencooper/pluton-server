package net.aidencooper.pluton_server.security.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController 
@RequestMapping("/api/v1/test")
public class TestController {
    @GetMapping()
    public String getTest() {
        return "Hello World!";
    }
}
