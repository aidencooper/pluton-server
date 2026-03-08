package net.aidencooper.pluton.server.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Responses {
    public static class Success {
        public static final String REGISTERED = "User is registered";
    }

    public static class Error {
        public static final String EMAIL_TAKEN = "That email address is already taken";
        public static final String INVALID_PASSWORD = "Invalid password";
        public static final String USERNAME_TAKEN = "That username is already taken";

    }
}
