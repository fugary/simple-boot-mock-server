CREATE TABLE t_mock_project_user
(
    id            int(11)      NOT NULL AUTO_INCREMENT comment '主键',
    user_name     varchar(255) NOT NULL comment '协作用户名',
    project_code  varchar(255) NOT NULL comment '项目代码',
    authorities   varchar(255) comment '操作权限(readable,writable,deletable等)',
    creator       varchar(255) comment '创建人',
    create_date   timestamp    NULL comment '创建时间',
    PRIMARY KEY (id)
);

CREATE INDEX idx_mock_project_user_code ON t_mock_project_user (project_code);
CREATE INDEX idx_mock_project_user_name ON t_mock_project_user (user_name);
