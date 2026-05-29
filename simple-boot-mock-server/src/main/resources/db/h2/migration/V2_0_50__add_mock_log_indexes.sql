create index idx_ml_create_date on t_mock_log(create_date);
create index idx_ml_type_date_id on t_mock_log(log_type, create_date, id);
create index idx_ml_status_date_id on t_mock_log(response_status_code, create_date, id);
create index idx_ml_ip_date_id on t_mock_log(ip_address, create_date, id);
