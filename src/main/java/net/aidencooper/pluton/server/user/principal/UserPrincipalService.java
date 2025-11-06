package net.aidencooper.pluton.server.user.principal;

import net.aidencooper.pluton.server.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserPrincipalService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserPrincipalService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new UserPrincipal(this.userRepository.findByUsername(username));
    }
}
