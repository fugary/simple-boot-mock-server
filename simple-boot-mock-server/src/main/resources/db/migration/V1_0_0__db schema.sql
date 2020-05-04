CREATE TABLE t_mock_data (
    id INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id INT(11) COMMENT '分组id',
    request_id INT(11) COMMENT '请求id',
    status INT(11) COMMENT '状态',
    status_code INT(11) COMMENT 'Http状态码',
    content_type VARCHAR(255) COMMENT '返回content类型',
    response_body TEXT COMMENT '返回内容',
    description VARCHAR(1024) COMMENT '描述信息',
    headers VARCHAR(2048) COMMENT '附加头信息',
    creator VARCHAR(255) COMMENT '创建人',
    create_date TIMESTAMP NULL COMMENT '创建时间',
    modifier VARCHAR(255) COMMENT '修改人',
    modify_date TIMESTAMP NULL COMMENT '修改时间',
    PRIMARY KEY (id)
);
CREATE TABLE t_mock_group (
    id INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_name VARCHAR(100) COMMENT '分组名称',
    group_path VARCHAR(255) COMMENT '映射路径',
    status TINYINT COMMENT '状态是否启用，0——禁用，1——启用',
    description VARCHAR(1024) COMMENT '描述信息',
    creator VARCHAR(255) COMMENT '创建人',
    create_date TIMESTAMP NULL COMMENT '创建时间',
    modifier VARCHAR(255) COMMENT '修改人',
    modify_date TIMESTAMP NULL COMMENT '修改时间',
    PRIMARY KEY (id)
);
CREATE TABLE t_mock_request (
    id INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    group_id INT(11) COMMENT '分组id',
    request_name VARCHAR(255) NOT NULL COMMENT '请求名称',
    request_path VARCHAR(1000) COMMENT '请求路径',
    method VARCHAR(50) COMMENT '请求方法',
    status INT(11) COMMENT '状态',
    description VARCHAR(1024) COMMENT '描述信息',
    creator VARCHAR(255) COMMENT '创建人',
    create_date TIMESTAMP NULL COMMENT '创建时间',
    modifier VARCHAR(255) COMMENT '修改人',
    modify_date TIMESTAMP NULL COMMENT '修改时间',
    PRIMARY KEY (id)
);
