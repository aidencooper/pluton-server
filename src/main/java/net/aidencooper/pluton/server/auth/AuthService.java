package net.aidencooper.pluton.server.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.aidencooper.pluton.server.auth.request.RegisterRequest;
import net.aidencooper.pluton.server.exception.EmailTakenException;
import net.aidencooper.pluton.server.exception.InvalidPasswordException;
import net.aidencooper.pluton.server.exception.UsernameTakenException;
import net.aidencooper.pluton.server.user.UserRepository;
import org.springframework.stereotype.Service;

@Service
@Getter
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;

    public void register(RegisterRequest registerRequest) throws InvalidPasswordException, EmailTakenException, UsernameTakenException {
        if(this.userRepository.findByUsername(registerRequest.username()).isPresent()) throw new UsernameTakenException();
        if(this.userRepository.findByEmail(registerRequest.email()).isPresent()) throw new EmailTakenException();
        if(this.getPasswordValidator().isValid(registerRequest.password())) throw new InvalidPasswordException();
    }
}
