DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id              BIGINT PRIMARY KEY,
    name            VARCHAR(100),
    label           VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT,
    role_id         BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
