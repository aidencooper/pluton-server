package net.aidencooper.pluton.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    public boolean isValid(String password) {
        if(password == null || password.isBlank()) return false;
        if (password.length() < minLength) return false;
        if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) return false;
        if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) return false;
        if (requireDigit && password.chars().noneMatch(Character::isDigit)) return false;
        if (requireSpecial && password.chars().allMatch((character) -> Character.isLetterOrDigit(character) || Character.isWhitespace(character))) return false;

        return true;
    }
}

// https://www.nist.gov/cybersecurity/how-do-i-create-good-password