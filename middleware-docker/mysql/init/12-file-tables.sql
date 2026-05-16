USE db_product;

CREATE TABLE IF NOT EXISTS file_records (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT NOT NULL,
    md5         VARCHAR(32) NOT NULL,
    bucket      VARCHAR(50) NOT NULL,
    object_key  VARCHAR(500) NOT NULL,
    url         VARCHAR(500),
    thumb_url   VARCHAR(500),
    uploader_id BIGINT COMMENT '上传者ID',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_uploader (uploader_id),
    INDEX idx_md5 (md5)
);
