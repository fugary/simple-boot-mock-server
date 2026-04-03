ALTER TABLE t_mock_group ADD COLUMN project_id int(11) comment '项目ID';

UPDATE t_mock_group
SET project_code = 'default',
    project_id = NULL
WHERE project_code IS NULL OR TRIM(project_code) = '';

INSERT INTO t_mock_project(user_name, project_code, project_name, status, creator, create_date)
SELECT '', 'default', N'默认项目', 1, 'flyway', NOW()
FROM dual
WHERE NOT EXISTS (
    SELECT 1 FROM t_mock_project WHERE project_code = 'default'
);

UPDATE t_mock_group g
    JOIN t_mock_project p ON p.project_code = g.project_code
SET g.project_id = p.id
WHERE g.project_id IS NULL
  AND g.project_code IS NOT NULL
  AND g.project_code <> 'default';

UPDATE t_mock_project_user pu
    JOIN t_mock_project p ON p.project_code = pu.project_code
SET pu.project_id = p.id
WHERE pu.project_id IS NULL
  AND pu.project_code IS NOT NULL
  AND pu.project_code <> 'default';

UPDATE t_mock_group
SET project_id = NULL
WHERE project_code = 'default';

UPDATE t_mock_project_user
SET project_id = NULL
WHERE project_code = 'default';

CREATE INDEX idx_mg_project_id ON t_mock_group (project_id);
CREATE INDEX idx_mg_user_pid_mod_status ON t_mock_group (user_name, project_id, modify_from, status);
