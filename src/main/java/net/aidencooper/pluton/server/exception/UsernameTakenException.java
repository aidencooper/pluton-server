package net.aidencooper.pluton.server.exception;

import net.aidencooper.pluton.server.constant.Responses;
import org.springframework.security.core.AuthenticationException;

public class UsernameTakenException extends AuthenticationException {
    public UsernameTakenException() {
        super(Responses.Error.USERNAME_TAKEN);
    }
}
