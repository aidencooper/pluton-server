package net.aidencooper.pluton.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordValidator {
    private final int minLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSpecial;

    public PasswordValidator(
            @Value("${security.password.min-length}") int minLength,
            @Value("${security.password.require-uppercase}") boolean requireUppercase,
            @Value("${security.password.require-lowercase}") boolean requireLowercase,
            @Value("${security.password.require-digit}") boolean requireDigit,
            @Value("${security.password.require-special}") boolean requireSpecial
    ) {
        this.minLength = minLength;
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSpecial = requireSpecial;
    }

    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();

        if(password == null || password.isBlank()) errors.add("Password cannot be blank");
        else {
            if (password.length() < minLength) errors.add("Password must be at least " + minLength + " characters long");
            if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) errors.add("Password must have at least one uppercase character");
            if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) errors.add("Password must have at least one lowercase character");
            if (requireDigit && password.chars().noneMatch(Character::isDigit)) errors.add("Password must have at least one digit");
            if (requireSpecial && password.chars().allMatch((character) -> Character.isLetterOrDigit(character) || Character.isWhitespace(character))) errors.add("Password must have at least one special character");
        }

        return errors;
    }
}

// https://www.nist.gov/cybersecurity/how-do-i-create-good-password