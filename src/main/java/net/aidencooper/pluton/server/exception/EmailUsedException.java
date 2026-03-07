package net.aidencooper.pluton.server.exception;

import org.springframework.security.core.AuthenticationException;

public class EmailUsedException extends AuthenticationException {
    public EmailUsedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public EmailUsedException(String msg) {
        super(msg);
    }
}
