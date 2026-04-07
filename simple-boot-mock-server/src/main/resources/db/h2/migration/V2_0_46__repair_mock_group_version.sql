UPDATE t_mock_group
SET data_version = 1
WHERE data_version IS NULL OR data_version = 0;
