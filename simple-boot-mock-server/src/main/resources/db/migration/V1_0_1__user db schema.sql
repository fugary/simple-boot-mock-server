CREATE TABLE t_mock_user (
    id INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_name VARCHAR(255) COMMENT '用户名',
    user_password VARCHAR(255) COMMENT '密码',
    status INT(11) COMMENT '状态',
    user_email VARCHAR(2049) COMMENT '邮箱',
    nick_name VARCHAR(255) COMMENT '昵称',
    creator VARCHAR(255) COMMENT '创建人',
    create_date TIMESTAMP NULL COMMENT '创建时间',
    modifier VARCHAR(255) COMMENT '修改人',
    modify_date TIMESTAMP NULL COMMENT '修改时间',
    PRIMARY KEY (id)
);
