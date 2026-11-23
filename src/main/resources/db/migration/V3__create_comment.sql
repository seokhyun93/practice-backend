CREATE TABLE comment (
                         id         BIGINT       NOT NULL AUTO_INCREMENT,
                         post_id    BIGINT       NOT NULL,
                         content    VARCHAR(500) NOT NULL,
                         author     VARCHAR(30)  NOT NULL,
                         created_at DATETIME(6)  NOT NULL,
                         updated_at DATETIME(6)  NOT NULL,
                         PRIMARY KEY (id),
                         CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES post (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
