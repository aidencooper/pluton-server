package net.aidencooper.pluton_server.security.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

public class User implements UserDetails {
    private final UUID id;
    private final String email;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;

    public User(
        UUID id,
        String email,
        String username,
        String password,
        Collection<? extends GrantedAuthority> authorities,
        boolean enabled
    ) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
    }

    public UUID getId() { return this.id; }
    public String getEmail() { return this.email; }
    @Override public String getUsername() { return this.username; }
    @Override public @Nullable String getPassword() { return this.password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return this.authorities; }
    @Override public boolean isEnabled() { return this.enabled; }

    public static UserBuilder with(String email, String username) {
        return new UserBuilder().email(email).username(username);
    }

    public static final class UserBuilder {
        private String email;
        private String username;
        private String password;
        private List<GrantedAuthority> authorities;
        private boolean enabled = true;

        public UserBuilder email(String email) {
            Assert.notNull(email, "email cannot be null");
            this.email = email;
            return this;
        }

        public UserBuilder username(String username) {
            Assert.notNull(username, "username cannot be null");
            this.username = username;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder roles(String... roles) {
            List<GrantedAuthority> authorities = new ArrayList<>(roles.length);
			for (String role : roles) {
				Assert.isTrue(!role.startsWith("ROLE_"),
						() -> role + " cannot start with ROLE_ (it is automatically added)");
				authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			}
			return authorities(authorities);
        }

        public UserBuilder authorities(GrantedAuthority... authorities) {
			Assert.notNull(authorities, "authorities cannot be null");
			return authorities(Arrays.asList(authorities));
		}

        public UserBuilder authorities(Collection<? extends GrantedAuthority> authorities) {
			Assert.notNull(authorities, "authorities cannot be null");
			this.authorities = new ArrayList<>(authorities);
			return this;
		}

        public UserBuilder enabled(boolean enabled) {
			this.enabled = enabled;
			return this;
		}

        public User build() {
            Assert.notNull(this.email, "email cannot be null");
            Assert.notNull(this.username, "username cannot be null");
            return new User(UUID.randomUUID(), this.email, this.username, this.password, this.authorities, this.enabled);
        }
    }
}
