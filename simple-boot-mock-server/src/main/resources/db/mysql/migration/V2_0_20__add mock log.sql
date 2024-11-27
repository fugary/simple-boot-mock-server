CREATE TABLE t_mock_log
(
    id          int(11) NOT NULL AUTO_INCREMENT,
    user_name   varchar(255),
    log_name    varchar(8192),
    data_id     int(11),
    log_message text,
    log_type    varchar(255),
    log_result  varchar(255),
    log_data    text,
    log_time    bigint(20),
    ip_address  varchar(1024),
    headers     text comment '附加头信息',
    exceptions  text,
    extend1     longtext,
    extend2     longtext,
    creator     varchar(255),
    create_date datetime    NULL,
    PRIMARY KEY (id)
);
