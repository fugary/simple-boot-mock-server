CREATE TABLE t_ai_config (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    config_name VARCHAR(255) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    api_key VARCHAR(4096),
    default_model VARCHAR(255) NOT NULL,
    is_default INT DEFAULT 0,
    status INT DEFAULT 1,
    creator VARCHAR(255),
    modifier VARCHAR(255),
    create_date TIMESTAMP NULL,
    modify_date TIMESTAMP NULL
);
CREATE INDEX idx_ai_config_user ON t_ai_config(user_name);
CREATE TABLE t_ai_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    config_id INT,
    config_name VARCHAR(255),
    model VARCHAR(255),
    prompt TEXT,
    response_text TEXT,
    error_message VARCHAR(512),
    duration_ms BIGINT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    status INT,
    creator VARCHAR(255),
    modifier VARCHAR(255),
    create_date TIMESTAMP NULL,
    modify_date TIMESTAMP NULL
);
CREATE INDEX idx_ai_log_user ON t_ai_log(user_name,
    id);
