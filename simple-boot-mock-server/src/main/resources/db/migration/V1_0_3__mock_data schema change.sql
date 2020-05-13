alter table t_mock_data add mock_params text COMMENT '保存参数信息';
alter table t_mock_request drop column request_name;