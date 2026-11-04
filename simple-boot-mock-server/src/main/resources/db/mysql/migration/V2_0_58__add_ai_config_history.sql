ALTER TABLE t_ai_config ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE t_ai_config ADD COLUMN modify_from INT;
CREATE INDEX idx_ai_config_history ON t_ai_config(modify_from, version);
ALTER TABLE t_ai_config ADD COLUMN default_slot INT GENERATED ALWAYS AS (CASE WHEN modify_from IS NULL AND is_default = 1 THEN 1 ELSE NULL END) STORED;
CREATE UNIQUE INDEX idx_ai_config_default_slot ON t_ai_config(default_slot);
