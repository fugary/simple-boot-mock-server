CREATE TABLE t_mock_data
(
    id            int(11) NOT NULL AUTO_INCREMENT comment '主键',
    group_id      int(11) comment '分组id',
    request_id    int(11) comment '请求id',
    status        int(11) comment '状态',
    status_code   int(11) comment 'Http状态码',
    default_flag  int(11) comment '是否默认响应数据',
    content_type  varchar(255) comment '返回content类型',
    mock_params   text comment '保存参数信息',
    headers       varchar(2048) comment '附加头信息',
    response_body text comment '返回内容',
    delay         int(11),
    description   varchar(1024) comment '描述信息',
    creator       varchar(255) comment '创建人',
    create_date   timestamp   NULL comment '创建时间',
    modifier      varchar(255) comment '修改人',
    modify_date   timestamp   NULL comment '修改时间',
    PRIMARY KEY (id)
);
CREATE TABLE t_mock_group
(
    id          int(11) NOT NULL AUTO_INCREMENT comment '主键',
    group_name  varchar(100) comment '分组名称',
    group_path  varchar(255) comment '映射路径',
    status      tinyint comment '状态是否启用，0——禁用，1——启用',
    proxy_url   varchar(2048),
    delay       int(11),
    description varchar(1024) comment '描述信息',
    creator     varchar(255) comment '创建人',
    create_date timestamp   NULL comment '创建时间',
    modifier    varchar(255) comment '修改人',
    modify_date timestamp   NULL comment '修改时间',
    PRIMARY KEY (id)
);
CREATE TABLE t_mock_request
(
    id            int(11) NOT NULL AUTO_INCREMENT comment '主键',
    group_id      int(11) comment '分组id',
    request_path  varchar(1000) comment '请求路径',
    method        varchar(50) comment '请求方法',
    status        int(11) comment '状态',
    delay         int(11),
    description   varchar(1024) comment '描述信息',
    mock_params   text,
    match_pattern varchar(4096),
    creator       varchar(255) comment '创建人',
    create_date   timestamp   NULL comment '创建时间',
    modifier      varchar(255) comment '修改人',
    modify_date   timestamp   NULL comment '修改时间',
    PRIMARY KEY (id)
);
CREATE TABLE t_mock_user
(
    id            int(11) NOT NULL AUTO_INCREMENT comment '主键',
    user_name     varchar(255) comment '用户名',
    user_password varchar(255) comment '密码',
    status        int(11) comment '状态',
    user_email    varchar(2049) comment '邮箱',
    nick_name     varchar(255) comment '昵称',
    creator       varchar(255) comment '创建人',
    create_date   timestamp   NULL comment '创建时间',
    modifier      varchar(255) comment '修改人',
    modify_date   timestamp   NULL comment '修改时间',
    PRIMARY KEY (id)
);
