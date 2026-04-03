ALTER TABLE t_mock_project_user ADD COLUMN project_id int;

UPDATE t_mock_project_user
SET project_id = (
    SELECT MIN(p.id)
    FROM t_mock_project p
    WHERE p.project_code = t_mock_project_user.project_code
);

CREATE INDEX idx_mock_project_user_pid ON t_mock_project_user (project_id);
