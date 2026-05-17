DROP TABLE IF EXISTS file_records;

CREATE TABLE file_records (
    id              BIGINT PRIMARY KEY,
    file_name       VARCHAR(255),
    file_size       BIGINT,
    md5             VARCHAR(64),
    bucket          VARCHAR(100),
    object_key      VARCHAR(500),
    url             VARCHAR(500),
    thumb_url       VARCHAR(500),
    uploader_id     BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
