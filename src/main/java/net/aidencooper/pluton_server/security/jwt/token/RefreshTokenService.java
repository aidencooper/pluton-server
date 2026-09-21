package net.aidencooper.pluton_server.security.jwt.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service 
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 64;
    private static final long EXPIRY_DAYS = 30;

    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String createToken(String username) {
        UUID userId = this.getUserId(username);

        String rawToken = this.generateRawToken();
        String tokenHash = this.hash(rawToken);
        Instant expiresAt = Instant.now().plus(EXPIRY_DAYS, ChronoUnit.DAYS);

        this.jdbcTemplate.update(
            "INSERT INTO refresh_tokens (id, user_id, token_hash, expires_at) VALUES (?, ?, ?, ?)", UUID.randomUUID(), userId, tokenHash, Timestamp.from(expiresAt)
        );

        return rawToken;
    }

    public String validateAndRotate(String rawToken) {
        String tokenHash = this.hash(rawToken);

        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(
            "SELECT u.username AS username, rt.expires_at AS expires_at, rt.revoked AS revoked " +
            "FROM refresh_tokens rt " +
            "JOIN users u ON u.id = rt.user_id " +
            "WHERE rt.token_hash = ?", tokenHash);
        
        if(rows.isEmpty()) throw new InvalidRefreshTokenException("Refresh token not recognized");
        
        // Only one row because rt.token_hash is unique
        Map<String, Object> row = rows.get(0);
        
        boolean revoked = (boolean) row.get("revoked");
        Instant expiresAt = ((Timestamp) row.get("expires_at")).toInstant();
        String username = (String) row.get("username");

        if(revoked) throw new InvalidRefreshTokenException("Refresh token has been revoked");
        if(expiresAt.isBefore(Instant.now())) throw new InvalidRefreshTokenException("Refresh token has expired");

        this.jdbcTemplate.update("UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = ?", tokenHash);
        return username;
    }

    public void revoke(String rawToken) {
        this.jdbcTemplate.update(
            "UPDATE refresh_tokens SET revoked = TRUE WHERE token_hash = ?", this.hash(rawToken)
        );
    }

    public void revokeAll(String username) {
        UUID userId = this.getUserId(username);

        this.jdbcTemplate.update(
            "UPDATE refresh_tokens SET revoked = TRUE WHERE user_id = ?", userId
        );
    }

    private UUID getUserId(String username) {
        return this.jdbcTemplate.queryForObject(
            "SELECT id FROM users WHERE username = ?", UUID.class, username);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hashed);
        } catch(NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }
}
