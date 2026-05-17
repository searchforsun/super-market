DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

CREATE TABLE users (
    id              BIGINT PRIMARY KEY,
    phone           VARCHAR(20),
    email           VARCHAR(100),
    password_hash   VARCHAR(255),
    nickname        VARCHAR(100),
    avatar_url      VARCHAR(500),
    real_name       VARCHAR(50),
    id_card         VARCHAR(20),
    status          INT DEFAULT 1,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
