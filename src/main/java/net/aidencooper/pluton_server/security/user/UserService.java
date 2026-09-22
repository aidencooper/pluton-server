package net.aidencooper.pluton_server.security.user;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

public class UserService implements UserDetailsService {
    private final JdbcTemplate jdbcTemplate;

    public UserService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User loadUserByUsername(final String username) throws UsernameNotFoundException {
        List<Map<String, Object>> rows = this.jdbcTemplate.queryForList(
            "SELECT id, email, username, password, enabled, expired, locked, password_expired, created_at FROM users WHERE username = ?", username
        );

        if (rows.isEmpty())
            throw new UsernameNotFoundException("No user found with username: " + username);

        Map<String, Object> row = rows.get(0);

        List<? extends GrantedAuthority> authorities = this.jdbcTemplate.queryForList(
            "SELECT authority FROM authorities WHERE username = ?", String.class, username
        ).stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
        
        return new User(
            (UUID) row.get("id"),
            (String) row.get("email"),
            (String) row.get("username"),
            (String) row.get("password"),
            authorities,
            (boolean) row.get("enabled"),
            (boolean) row.get("expired"),
            (boolean) row.get("locked"),
            (boolean) row.get("password_expired"),
            (Timestamp) row.get("created_at")
        );
    }

    @Transactional
    public void createUser(final User user) {
        this.validateUser(user);
        this.jdbcTemplate.update(
            "INSERT INTO users (id, email, username, password, enabled, expired, locked, password_expired, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", 
            user.getId(),
            user.getEmail(), 
            user.getUsername(), 
            user.getPassword(), 
            user.isEnabled(),
            !user.isAccountNonExpired(),
            !user.isAccountNonLocked(),
            !user.isCredentialsNonExpired(),
            user.getCreatedAt()
        );

        this.jdbcTemplate.update(
            "INSERT INTO user_profiles (user_id, display_name, avatar_url) VALUES (?, ?, ?)",
            user.getId(),
            null,
            null
        );

        for(GrantedAuthority authority : user.getAuthorities()) {
            this.jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)", 
                user.getUsername(), 
                authority.getAuthority()
            );
        }
    }

    public boolean userExists(final String username) {
        Integer count = this.jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        
        return count != null && count > 0;
    }

    private void validateUser(final User user) {
        Assert.hasText(user.getEmail(), "Email may not be empty or null");
		Assert.hasText(user.getUsername(), "Username may not be empty or null");
		validateAuthorities(user.getAuthorities());
	}

	private void validateAuthorities(final Collection<? extends GrantedAuthority> authorities) {
		Assert.notNull(authorities, "Authorities list must not be null");
		for (GrantedAuthority authority : authorities) {
			Assert.notNull(authority, "Authorities list contains a null entry");
			Assert.hasText(authority.getAuthority(), "getAuthority() method must return a non-empty string");
		}
	}
}
