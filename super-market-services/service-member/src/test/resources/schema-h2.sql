DROP TABLE IF EXISTS members;

CREATE TABLE members (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT,
    level           INT DEFAULT 1,
    points          INT DEFAULT 0,
    total_points    INT DEFAULT 0,
    growth_value    INT DEFAULT 0,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
