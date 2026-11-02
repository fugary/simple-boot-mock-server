ALTER TABLE t_ai_log ADD COLUMN raw_response CLOB;
ALTER TABLE t_ai_log ADD COLUMN execution_detail CLOB;
ALTER TABLE t_ai_log ADD COLUMN provider VARCHAR(32);
