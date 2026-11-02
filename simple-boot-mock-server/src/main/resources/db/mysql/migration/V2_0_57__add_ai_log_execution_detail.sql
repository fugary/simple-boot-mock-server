ALTER TABLE t_ai_log ADD COLUMN raw_response MEDIUMTEXT;
ALTER TABLE t_ai_log ADD COLUMN execution_detail MEDIUMTEXT;
ALTER TABLE t_ai_log ADD COLUMN provider VARCHAR(32);
