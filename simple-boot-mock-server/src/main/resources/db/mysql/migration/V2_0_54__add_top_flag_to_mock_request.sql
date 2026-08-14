ALTER TABLE t_mock_request ADD COLUMN top_flag bit DEFAULT 0;
UPDATE t_mock_request SET top_flag = 0 WHERE top_flag IS NULL;
