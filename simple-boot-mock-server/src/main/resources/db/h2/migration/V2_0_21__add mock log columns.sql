ALTER TABLE t_mock_log ADD COLUMN request_url text;
ALTER TABLE t_mock_log ADD COLUMN mock_group_path varchar(255);
ALTER TABLE t_mock_log ADD COLUMN response_body longtext;
