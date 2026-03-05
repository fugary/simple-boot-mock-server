-- mock group
create index idx_mg_group_path_mod on t_mock_group(group_path, modify_from);
create index idx_mg_user_proj_mod_status on t_mock_group(user_name, project_code, modify_from, status);
create index idx_mg_modify_from_version on t_mock_group(modify_from, data_version);

-- mock request
create index idx_mr_group_method_mod_status on t_mock_request(group_id, method, modify_from, status);
create index idx_mr_group_mod on t_mock_request(group_id, modify_from);
create index idx_mr_modify_from_version on t_mock_request(modify_from, data_version);
create index idx_mr_creator_mod_status on t_mock_request(creator, modify_from, status);

-- mock data
create index idx_md_request_mod_status_code on t_mock_data(request_id, modify_from, status, status_code);
create index idx_md_request_mod_default on t_mock_data(request_id, modify_from, default_flag);
create index idx_md_modify_from_version on t_mock_data(modify_from, data_version);
create index idx_md_creator_mod_status on t_mock_data(creator, modify_from, status);

-- mock schema
create index idx_ms_request_data on t_mock_schema(request_id, data_id);
create index idx_ms_group_request on t_mock_schema(group_id, request_id);
create index idx_ms_data on t_mock_schema(data_id);

-- mock project
create index idx_mp_user_code on t_mock_project(user_name, project_code);
create index idx_mp_status_public_user on t_mock_project(status, public_flag, user_name);
create index idx_mp_code on t_mock_project(project_code);

-- mock user
create index idx_mu_user_name on t_mock_user(user_name);

-- mock log
create index idx_ml_user_date on t_mock_log(user_name, create_date);
create index idx_ml_result_user_date on t_mock_log(log_result, user_name, create_date);
create index idx_ml_data_user_date on t_mock_log(data_id, user_name, create_date);
create index idx_ml_group_path_date on t_mock_log(mock_group_path, create_date);
