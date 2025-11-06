package net.aidencooper.pluton.server.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(final String username, final String password) {
        if(this.userRepository.findByUsername(username) != null) throw new IllegalStateException("Username is taken");

        return this.userRepository.save(new User(username, this.passwordEncoder.encode(password)));
    }
}
