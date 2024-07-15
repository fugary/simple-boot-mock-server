ALTER TABLE
    t_mock_group
ADD COLUMN project_code varchar(255);

CREATE TABLE t_mock_project
(
    id           int(11)   NOT NULL AUTO_INCREMENT comment '主键',
    user_name    varchar(255) comment '所属用户',
    project_code varchar(255) comment '项目代码',
    project_name varchar(255) comment '项目名称',
    status       tinyint(3) comment '状态是否启用，0——禁用，1——启用',
    description  varchar(1024) comment '描述信息',
    creator      varchar(255) comment '创建人',
    create_date  timestamp NULL comment '创建时间',
    modifier     varchar(255) comment '修改人',
    modify_date  timestamp NULL comment '修改时间',
    PRIMARY KEY (id)
);
