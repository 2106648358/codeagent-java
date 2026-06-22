CREATE TABLE sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    title VARCHAR(512),
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE messages (
    id INTEGER PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    model_id VARCHAR(255),
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    timestamp BIGINT NOT NULL,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE generation_tasks (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    requirements TEXT NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    completed_at BIGINT,
    FOREIGN KEY (session_id) REFERENCES sessions(id)
);

CREATE TABLE content_fragments (
    id INTEGER PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    relative_path VARCHAR(1024) NOT NULL,
    content TEXT NOT NULL,
    start_line INTEGER,
    end_line INTEGER,
    description TEXT,
    FOREIGN KEY (task_id) REFERENCES generation_tasks(id)
);

CREATE TABLE telemetry_events (
    id INTEGER PRIMARY KEY,
    command_type VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    user_id VARCHAR(255),
    tenant_id VARCHAR(255),
    model_id VARCHAR(255),
    duration_ms BIGINT NOT NULL,
    content_type VARCHAR(32),
    file_extension VARCHAR(32),
    file_count INTEGER,
    success INTEGER NOT NULL,
    error_message TEXT,
    timestamp BIGINT NOT NULL
);
