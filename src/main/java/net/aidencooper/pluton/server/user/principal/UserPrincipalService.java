package net.aidencooper.pluton.server.user.principal;

import net.aidencooper.pluton.server.exception.EmailNotFoundException;
import net.aidencooper.pluton.server.user.User;
import net.aidencooper.pluton.server.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class UserPrincipalService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserPrincipalService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Treating email as username
    @Override
    public UserDetails loadUserByUsername(String email) throws EmailNotFoundException {
        User user = this.userRepository
                .findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("No user found with email: " + email));

        return new UserPrincipal(user);
    }
}
