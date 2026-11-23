CREATE TABLE post
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;