package net.aidencooper.pluton.server.user;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User createUser(@RequestParam String username, @RequestParam String password) {
        return this.userService.register(username, password);
    }
}
