package net.aidencooper.pluton.server.exception;

import net.aidencooper.pluton.server.constant.Responses;
import org.springframework.security.core.AuthenticationException;

public class EmailTakenException extends AuthenticationException {
    public EmailTakenException() {
        super(Responses.Error.EMAIL_TAKEN);
    }
}
