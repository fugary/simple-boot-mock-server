create unique index uk_ms_group_code on t_mock_scenario(group_id, scenario_code);
create index idx_ms_group_status on t_mock_scenario(group_id, status);
create index idx_mg_active_scenario_code on t_mock_group(active_scenario_code);
create index idx_mr_group_scenario_method_status_mod
    on t_mock_request(group_id, scenario_code, method, status, modify_from);
