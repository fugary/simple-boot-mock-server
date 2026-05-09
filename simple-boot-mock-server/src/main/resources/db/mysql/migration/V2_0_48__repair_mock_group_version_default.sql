UPDATE t_mock_group
SET data_version = 1
WHERE data_version IS NULL OR data_version < 1;

ALTER TABLE t_mock_group MODIFY COLUMN data_version INT(11) DEFAULT 1 COMMENT 'Data Version';
