CREATE TABLE t_mock_schema
(
    id                   int(10) NOT NULL AUTO_INCREMENT,
    request_id           int(11),
    data_id              int(11),
    parameters_schema    text,
    request_body_schema  text,
    response_body_schema text,
    create_date          timestamp   NULL comment '创建时间',
    modifier             varchar(255) comment '修改人',
    modify_date          timestamp   NULL comment '修改时间',
    creator              varchar(255),
    PRIMARY KEY (id)
);
