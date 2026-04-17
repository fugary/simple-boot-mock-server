create index idx_ml_name_user_date on t_mock_log(log_name(255), user_name, create_date);
create index idx_ml_name_date on t_mock_log(log_name(255), create_date);
