package net.aidencooper.pluton_server.security.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.notIn;

import java.net.http.HttpRequest;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers 
public class AuthControllerIT {
    private static final String LOGIN_ENDPOINT = "/api/v1/auth/login";
    private static final String REGISTER_ENDPOINT = "/api/v1/auth/register";
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

    @BeforeEach 
    void cleanDb() {
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
    void login_withValidCredentials_returnsJwt() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);

        ResponseEntity<String> response = this.login(username, password);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotBlank();
        assertThat(response.getBody().split("\\.")).hasSize(3); // 3 parts seperated by dots
    }

    @Test 
    void login_tokenContainsCorrectSubjectAndScope() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);

        String token = this.login(username, password).getBody();

        String[] parts = token.split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(parts[1]));

        assertThat(json).contains("\"sub\":\"" + username + "\"");
        assertThat(json).contains("ROLE_USER");
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

    // Protected Endpoint
    @Test 
    void issuedToken_isAcceptedAsBearerTokenOnProtectedEndpoint_returnsOk() {
        final String username = "test";
        final String password = "password";

        this.register(username, password);
        String token = this.login(username, password).getBody();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
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
