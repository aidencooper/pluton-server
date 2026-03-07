package net.aidencooper.pluton.server.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RestResponse {
    public static class Success {
        public static final String REGISTERED = "User is registered";
    }

    public static class Error {
        public static final String INVALID_PASSWORD = "Invalid password";
        public static final String USER_EXISTS = "That email address is already taken";
    }
}
