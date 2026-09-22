package net.aidencooper.pluton_server.security.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers 
public class AuthControllerIT {
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
    private static final String REFRESH_ENDPOINT = "/api/v1/auth/refresh";
    private static final String LOGOUT_ENDPOINT = "/api/v1/auth/logout";
    private static final String TEST_PROTECTED_ENDPOINT = "/api/v1/test";

    @Container 
    @ServiceConnection 
    private static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:latest");

    @Autowired 
    private TestRestTemplate restTemplate;

    @Autowired 
    private JdbcTemplate jdbcTemplate;

    @Autowired 
    private JwtEncoder jwtEncoder;

    @Autowired 
    private ObjectMapper objectMapper;

    @BeforeEach 
    void cleanDb() {
        jdbcTemplate.execute("DELETE FROM refresh_tokens");
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM users");
    }  

    // Helpers

    private ResponseEntity<String> register(String username, String password) {
        String url = REGISTER_ENDPOINT + "?username=" + username + "&password=" + password;
        return this.restTemplate.postForEntity(url, null, String.class);
    }

    private ResponseEntity<String> login(String username, String password) {
        String url = LOGIN_ENDPOINT;

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        return this.restTemplate.postForEntity(url, request, String.class);
    }

    private TokenResponse loginAndParse(String username, String password) {
        String body = this.login(username, password).getBody();
        return this.objectMapper.readValue(body, TokenResponse.class);
    }

    private ResponseEntity<String> refresh(String refreshToken) {
        String url = REFRESH_ENDPOINT + "?refreshToken=" + refreshToken;
        return this.restTemplate.postForEntity(url, null, String.class);
    }

    private ResponseEntity<Void> logout(String refreshToken) {
        String url = LOGOUT_ENDPOINT + "?refreshToken=" + refreshToken;
        return this.restTemplate.postForEntity(url, null, Void.class);
    }

    private String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().encodeToString(hashed);
        } catch(NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    // Register

    @Test 
    void register_newUser_returns201AndPersistsUser() {
        final String username = "test";
        final String password = "password";
        
        ResponseEntity<String> response = this.register(username, password);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains(username);

        Integer count = this.jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        
        assertThat(count).isEqualTo(1);
    }

    @Test 
    void register_grantsRoleUser() {
        final String username = "test";
        final String password = "password";
        
        this.register(username, password);

        String authority = this.jdbcTemplate.queryForObject(
            "SELECT authority FROM authorities WHERE username = ?", String.class, username);
        
        assertThat(authority).isEqualTo("ROLE_USER");
    }

    @Test 
    void register_storesBCryptEncodedPassword_notPlainText() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);

        String storedPassword = this.jdbcTemplate.queryForObject(
            "SELECT password FROM users WHERE username = ?", String.class, username);
        
        assertThat(storedPassword).startsWith("$2"); // BCrypt
    }

    @Test 
    void register_duplicateUsername_returns409() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        ResponseEntity<String> secondRegister = this.register(username, password);

        assertThat(secondRegister.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Integer count = this.jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        
        assertThat(count).isEqualTo(1);
    }

    // Login

    @Test 
    void login_withValidCredentials_returnsAccessAndRefreshTokens() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);

        TokenResponse tokens = this.loginAndParse(username, password);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.accessToken().split("\\.")).hasSize(3);
        assertThat(tokens.refreshToken()).isNotBlank();
    }

    @Test 
    void login_tokenContainsCorrectSubjectAndScope() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);

        TokenResponse tokens = this.loginAndParse(username, password);

        String[] parts = tokens.accessToken().split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(parts[1]));

        assertThat(json).contains("\"sub\":\"" + username + "\"");
        assertThat(json).contains("ROLE_USER");
    }

    @Test 
    void login_persistsRefreshTokenHash() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        String expectedHash = this.hashRefreshToken(tokens.refreshToken());

        Integer count = this.jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM refresh_tokens WHERE token_hash = ?", Integer.class, expectedHash);
        assertThat(count).isEqualTo(1);

        String storedHash = this.jdbcTemplate.queryForObject(
            "SELECT token_hash FROM refresh_tokens WHERE token_hash = ?", String.class, expectedHash);
        assertThat(storedHash).isNotEqualTo(tokens.refreshToken());
    }

    @Test 
    void login_withWrongPassword_returnsUnauthorized() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);

        ResponseEntity<String> response = this.login(username, "wrong" + password);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test 
    void login_withNonexistentUser_returnsUnauthorized() {
        final String username = "test";
        final String password = "password";

        ResponseEntity<String> response = this.login(username, password);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withoutCredentials_returnsUnauthorized() {
        ResponseEntity<String> response = this.restTemplate.postForEntity(LOGIN_ENDPOINT, null, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_withMalformedBasicAuthHeader_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic %InvalidBase64BecauseOf%Sign");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.postForEntity(LOGIN_ENDPOINT, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // Refresh

    @Test 
    void refresh_withValidToken_returnsNewAccessAndRefreshTokens() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        ResponseEntity<String> response = this.refresh(tokens.refreshToken());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TokenResponse refreshedTokens = this.objectMapper.readValue(response.getBody(), TokenResponse.class);

        assertThat(refreshedTokens.accessToken()).isNotBlank();
        assertThat(refreshedTokens.refreshToken()).isNotBlank();
        assertThat(refreshedTokens.refreshToken()).isNotEqualTo(tokens.refreshToken());
    }

    @Test 
    void refresh_newAccessToken_isAcceptedOnProtectedEndpoint() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        ResponseEntity<String> response = this.refresh(tokens.refreshToken());
        TokenResponse refreshedTokens = this.objectMapper.readValue(response.getBody(), TokenResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(refreshedTokens.accessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> protectedResponse = this.restTemplate.exchange(
            TEST_PROTECTED_ENDPOINT, HttpMethod.GET, request, String.class);
        
        assertThat(protectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void refresh_rotatesToken_oldTokenCannotBeReused() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        ResponseEntity<String> refresh1 = this.refresh(tokens.refreshToken());
        assertThat(refresh1.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> refresh2 = this.refresh(tokens.refreshToken());
        assertThat(refresh2.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test 
    void refresh_withUnrecognizedToken_returnsUnauthorized() {
        ResponseEntity<String> response = this.refresh("this-token-was-never-issued");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    void refresh_withExpiredToken_returnsUnauthorized() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        String hash = this.hashRefreshToken(tokens.refreshToken());
        Timestamp past = Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS));
        this.jdbcTemplate.update(
            "UPDATE refresh_tokens SET expires_at = ? WHERE token_hash = ?", past, hash);

        ResponseEntity<String> response = this.refresh(tokens.refreshToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void refresh_afterUserLogsInTwice_bothRefreshTokensRemainIndependentlyValid() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens1 = this.loginAndParse(username, password);
        TokenResponse tokens2 = this.loginAndParse(username, password);

        assertThat(tokens1.refreshToken()).isNotEqualTo(tokens2.refreshToken());

        ResponseEntity<String> refresh1 = this.refresh(tokens1.refreshToken());
        ResponseEntity<String> refresh2 = this.refresh(tokens2.refreshToken());

        assertThat(refresh1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refresh2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Logout

    @Test
    void logout_revokesToken_subsequentRefreshFails() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        ResponseEntity<Void> logoutResponse = this.logout(tokens.refreshToken());
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> refreshResponse = this.refresh(tokens.refreshToken());
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_withUnknownToken_stillReturnsOk() {
        ResponseEntity<Void> response = this.logout("never-issued-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void logout_doesNotAffectOtherSessionsForSameUser() throws Exception {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens1 = this.loginAndParse(username, password);
        TokenResponse tokens2 = this.loginAndParse(username, password);

        this.logout(tokens1.refreshToken());

        ResponseEntity<String> refresh1 = this.refresh(tokens1.refreshToken());
        ResponseEntity<String> refresh2 = this.refresh(tokens2.refreshToken());

        assertThat(refresh1.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh2.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // Protected Endpoint
    @Test 
    void issuedToken_isAcceptedAsBearerTokenOnProtectedEndpoint_returnsOk() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        TokenResponse tokens = this.loginAndParse(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.exchange(TEST_PROTECTED_ENDPOINT, HttpMethod.GET, request, String.class); // .getForEntity doesn't accept request

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test 
    void protectedEndpoint_withoutToken_returnsUnauthorized() {
        ResponseEntity<String> response = this.restTemplate.getForEntity(TEST_PROTECTED_ENDPOINT, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test 
    void protectedEndpoint_withMalformedBearerToken_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not.a.real-jwt");
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.exchange(TEST_PROTECTED_ENDPOINT, HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpoint_withExpiredJwt_returnsUnauthorized() {
        Instant past = Instant.now().minus(2, ChronoUnit.HOURS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("self")
            .issuedAt(past)
            .expiresAt(past.plus(1, ChronoUnit.HOURS))
            .subject("test")
            .claim("scope", "ROLE_USER")
            .build();
        
        String expiredToken = this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(expiredToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = this.restTemplate.exchange(TEST_PROTECTED_ENDPOINT, HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
