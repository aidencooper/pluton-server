package net.aidencooper.pluton.server.auth;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.aidencooper.pluton.server.auth.request.LoginRequest;
import net.aidencooper.pluton.server.auth.request.SignupRequest;
import net.aidencooper.pluton.server.user.User;
import net.aidencooper.pluton.server.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public void login(LoginRequest request) {
        Authentication authentication = this.authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    request.email(),
                    request.password()
        ));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void signup(SignupRequest request) {
        User user = new User(
                request.email(),
                this.passwordEncoder.encode(request.password())
        );

        this.userRepository.save(user);


    }
}
