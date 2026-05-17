DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS notify_templates;

CREATE TABLE notify_templates (
    id              BIGINT PRIMARY KEY,
    code            VARCHAR(100),
    name            VARCHAR(255),
    channel         INT DEFAULT 1,
    title           VARCHAR(500),
    content         VARCHAR(4000),
    status          INT DEFAULT 1,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT,
    template_id     BIGINT,
    channel         INT DEFAULT 1,
    title           VARCHAR(500),
    content         VARCHAR(4000),
    target          VARCHAR(255),
    status          INT DEFAULT 1,
    send_status     INT DEFAULT 0,
    send_at         TIMESTAMP,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
