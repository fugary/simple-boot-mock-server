ALTER TABLE t_mock_log ADD COLUMN diagnose_id varchar(64);
CREATE INDEX idx_ml_diagnose_id ON t_mock_log(diagnose_id);
