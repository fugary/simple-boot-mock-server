ALTER TABLE t_mock_log ADD COLUMN response_status_code int;
ALTER TABLE t_mock_log ADD COLUMN response_content_type varchar(1024);
ALTER TABLE t_mock_log ADD COLUMN diagnose_data longtext;
