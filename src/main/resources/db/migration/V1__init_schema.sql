-- Matched from spring-security-core-7.1.1.jar/org.springframework.security.core.userdetails.jdbc/users.ddl
-- Case insensitive
-- Primary key changed from username to added id column

CREATE TABLE users (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true,
    expired BOOLEAN NOT NULL DEFAULT false,
    locked BOOLEAN NOT NULL DEFAULT false,
    password_expired BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username)
);

CREATE TABLE oauth_accounts (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,

    UNIQUE (provider, provider_user_id)
);
CREATE INDEX ix_oauth_accounts_user_id ON oauth_accounts (user_id);

CREATE TABLE user_profiles (
    user_id UUID NOT NULL PRIMARY KEY REFERENCES users (id),
    display_name VARCHAR(40),
    avatar_url VARCHAR(500)
);
CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);