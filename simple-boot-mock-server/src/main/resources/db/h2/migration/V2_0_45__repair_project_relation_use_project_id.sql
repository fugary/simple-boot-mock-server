ALTER TABLE t_mock_group ADD COLUMN project_id int;

UPDATE t_mock_group
SET project_code = 'default',
    project_id = NULL
WHERE project_code IS NULL OR TRIM(project_code) = '';

INSERT INTO t_mock_project(user_name, project_code, project_name, status, creator, create_date)
SELECT '', 'default', '默认项目', 1, 'flyway', CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM t_mock_project WHERE project_code = 'default'
);

UPDATE t_mock_group
SET project_id = (
    SELECT MIN(p.id)
    FROM t_mock_project p
    WHERE p.project_code = t_mock_group.project_code
)
WHERE project_id IS NULL
  AND project_code IS NOT NULL
  AND project_code <> 'default';

UPDATE t_mock_project_user
SET project_id = (
    SELECT MIN(p.id)
    FROM t_mock_project p
    WHERE p.project_code = t_mock_project_user.project_code
)
WHERE project_id IS NULL
  AND project_code IS NOT NULL
  AND project_code <> 'default';

UPDATE t_mock_group
SET project_id = NULL
WHERE project_code = 'default';

UPDATE t_mock_project_user
SET project_id = NULL
WHERE project_code = 'default';

CREATE INDEX idx_mg_project_id ON t_mock_group (project_id);
CREATE INDEX idx_mg_user_pid_mod_status ON t_mock_group (user_name, project_id, modify_from, status);
