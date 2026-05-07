CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(64)  NOT NULL,
    email       VARCHAR(128) NOT NULL,
    age         INT          NOT NULL DEFAULT 0,
    status      INT          NOT NULL DEFAULT 1,
    is_deleted  TINYINT      NOT NULL DEFAULT 0,
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   VARCHAR(64),
    update_by   VARCHAR(64),
    version     INT          NOT NULL DEFAULT 0
);
