CREATE TABLE IF NOT EXISTS t_metric_event (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    event_type  VARCHAR(64)  NOT NULL,
    payload     VARCHAR(512),
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
