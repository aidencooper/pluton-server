-- Matched from spring-security-core-7.1.1.jar/org.springframework.security.core.userdetails.jdbc/users.ddl
-- Case insensitive

CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(50) NOT NULL PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users (username)
);

-- Rejected: (username=test authority=role_user | username=test authority=role_user)
-- Accepted: (username=test authority=role_user | username=test authority=role_admin)
CREATE UNIQUE INDEX IF NOT EXISTS ix_auth_username ON authorities (username, authority);