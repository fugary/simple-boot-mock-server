UPDATE t_mock_schema
SET GROUP_ID = (
    SELECT GROUP_ID FROM t_mock_request WHERE t_mock_request.ID = t_mock_schema.REQUEST_ID
),
BODY_TYPE = 'CONTENT'
WHERE GROUP_ID IS NULL;
