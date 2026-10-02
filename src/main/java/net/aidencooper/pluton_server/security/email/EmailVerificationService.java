package net.aidencooper.pluton_server.security.email;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import net.aidencooper.pluton_server.security.crypto.TokenHasher;

@Service 
public class EmailVerificationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long EXPIRY_MINUTES = 10;

    private final JdbcTemplate jdbcTemplate;

    public EmailVerificationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String generateCode(UUID userId) {
        String rawCode = String.format("%06d", SECURE_RANDOM.nextInt(1000000));
        String codeHash = TokenHasher.hash(rawCode);
        Instant expiresAt = Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES);

        this.jdbcTemplate.update(
            "INSERT INTO email_verification_codes (id, user_id, code_hash, expires_at) VALUES (?, ?, ?, ?)",
            UUID.randomUUID(), userId, codeHash, Timestamp.from(expiresAt)
        );

        return rawCode;
    }

    public void verifyCode(UUID userId, String rawCode) {
        String codeHash = TokenHasher.hash(rawCode);

        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(
            "SELECT id, consumed, expires_at FROM email_verification_codes WHERE user_id = ? AND code_hash = ? ORDER BY created_at DESC LIMIT 1", 
            userId, codeHash
        );

        if(rows.isEmpty()) throw new InvalidVerificationCodeException("Incorrect verification code");

        Map<String, Object> row = rows.get(0);
        boolean consumed = (boolean) row.get("consumed");
        Instant expiresAt = ((Timestamp) row.get("expires_at")).toInstant();

        if(consumed) throw new InvalidVerificationCodeException("This code has already been used");
        if(expiresAt.isBefore(Instant.now())) throw new InvalidVerificationCodeException("This code has expired");
        
        UUID codeId = (UUID) row.get("id");
        this.jdbcTemplate.update(
            "UPDATE email_verification_codes SET consumed = TRUE WHERE id = ?",
            codeId
        );

        this.jdbcTemplate.update(
            "UPDATE users SET email_verified = TRUE WHERE id = ?",
            userId
        );
    }

    public Map<String, Object> findUserByEmail(String email) {
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(
            "SELECT id, email_verified FROM users WHERE email = ?", email
        );
        return rows.isEmpty() ? null : rows.get(0);
    }
}
