package net.aidencooper.pluton.server.auth;

import lombok.RequiredArgsConstructor;
import net.aidencooper.pluton.server.auth.request.RegisterRequest;
import net.aidencooper.pluton.server.exception.InvalidPasswordException;
import net.aidencooper.pluton.server.exception.UserExistsException;
import net.aidencooper.pluton.server.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;

    public void register(RegisterRequest registerRequest) throws InvalidPasswordException, UserExistsException {
        if(this.userRepository.findByEmail(registerRequest.email()).isEmpty()) throw new UserExistsException("User with the email " + registerRequest.email() + " already exists");

        List<String> passwordErrors = passwordValidator.validate(registerRequest.password());
        if(!passwordErrors.isEmpty()) throw new InvalidPasswordException(String.join(", ", passwordErrors));



    }
}
