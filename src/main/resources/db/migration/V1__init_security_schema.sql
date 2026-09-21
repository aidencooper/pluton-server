-- Matched from spring-security-core-7.1.1.jar/org.springframework.security.core.userdetails.jdbc/users.ddl
-- Case insensitive
-- Primary key changed from username to added id column

CREATE TABLE users (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username)
);

-- Rejected: (username=test authority=role_user | username=test authority=role_user)
-- Accepted: (username=test authority=role_user | username=test authority=role_admin)
CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);