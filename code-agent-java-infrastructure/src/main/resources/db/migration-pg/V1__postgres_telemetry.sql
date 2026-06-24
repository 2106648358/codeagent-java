-- =============================================================================
-- PostgreSQL 远程遥测表结构定义
-- 用途：存储从各客户端同步上来的非敏感遥测指标，
--       用于远程使用分析和产品改进（不含实际代码内容）。
-- 管理方式：Flyway 管理迁移，与 SQLite 本地存储的 migration 目录分离。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 租户表：记录使用该服务的组织/团队信息
-- -----------------------------------------------------------------------------
CREATE TABLE tenant (
    id              VARCHAR(64)     PRIMARY KEY,                    -- 租户唯一标识
    name            VARCHAR(255)    NOT NULL,                       -- 租户名称
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),         -- 首次接入时间
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()          -- 最近更新时间
);

-- -----------------------------------------------------------------------------
-- 开发者表：记录租户下的个人开发者信息
-- -----------------------------------------------------------------------------
CREATE TABLE developer (
    id              VARCHAR(64)     PRIMARY KEY,                    -- 开发者唯一标识（与本地 userId 对应）
    tenant_id       VARCHAR(64)     NOT NULL REFERENCES tenant(id), -- 所属租户
    name            VARCHAR(255),                                   -- 开发者名称（可选）
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),         -- 首次出现时间
    last_active_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()          -- 最近活跃时间
);
CREATE INDEX idx_developer_tenant ON developer(tenant_id);

-- -----------------------------------------------------------------------------
-- 会话指标表：每个会话维度的聚合遥测数据
-- -----------------------------------------------------------------------------
CREATE TABLE session_metrics (
    id              BIGSERIAL       PRIMARY KEY,                    -- 自增主键
    tenant_id       VARCHAR(64)     NOT NULL,                       -- 租户 ID
    user_id         VARCHAR(64)     NOT NULL,                       -- 用户 ID
    session_id      VARCHAR(64)     NOT NULL,                       -- 会话 ID
    command_count   INTEGER         NOT NULL DEFAULT 0,             -- 命令执行次数
    total_duration_ms  BIGINT       NOT NULL DEFAULT 0,             -- 总耗时（毫秒）
    total_tokens    INTEGER         NOT NULL DEFAULT 0,             -- 总 Token 消耗
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),         -- 记录创建时间
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()          -- 最近更新时间
);
CREATE INDEX idx_session_metrics_tenant ON session_metrics(tenant_id);
CREATE INDEX idx_session_metrics_session ON session_metrics(session_id);

-- -----------------------------------------------------------------------------
-- LLM 用量表：每次 LLM 调用的 Token 消耗和耗时
-- -----------------------------------------------------------------------------
CREATE TABLE llm_usage (
    id              BIGSERIAL       PRIMARY KEY,                    -- 自增主键
    tenant_id       VARCHAR(64)     NOT NULL,                       -- 租户 ID
    user_id         VARCHAR(64)     NOT NULL,                       -- 用户 ID
    session_id      VARCHAR(64),                                    -- 关联会话 ID
    model_id        VARCHAR(255),                                   -- 模型标识（如 openai:gpt-4.1）
    prompt_tokens   INTEGER,                                        -- 输入 Token 数
    completion_tokens INTEGER,                                      -- 输出 Token 数
    total_tokens    INTEGER,                                        -- 总 Token 数
    duration_ms     BIGINT          NOT NULL,                       -- 调用耗时（毫秒）
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()          -- 调用时间
);
CREATE INDEX idx_llm_usage_tenant ON llm_usage(tenant_id);
CREATE INDEX idx_llm_usage_created ON llm_usage(created_at);

-- -----------------------------------------------------------------------------
-- 功能事件表：记录每次 CLI 命令执行的遥测事件
-- -----------------------------------------------------------------------------
CREATE TABLE feature_event (
    id              BIGSERIAL       PRIMARY KEY,                    -- 自增主键
    tenant_id       VARCHAR(64)     NOT NULL,                       -- 租户 ID
    user_id         VARCHAR(64)     NOT NULL,                       -- 用户 ID
    session_id      VARCHAR(64),                                    -- 关联会话 ID
    command_type    VARCHAR(64)     NOT NULL,                       -- 命令类型（chat/generate/explain）
    content_type    VARCHAR(32),                                    -- 内容类型（CODE/MARKDOWN/XML 等）
    file_extension  VARCHAR(32),                                    -- 目标文件扩展名
    file_count      INTEGER,                                        -- 涉及文件数量
    duration_ms     BIGINT          NOT NULL,                       -- 执行耗时（毫秒）
    success         BOOLEAN         NOT NULL DEFAULT TRUE,          -- 是否成功
    error_message   TEXT,                                           -- 失败时的错误信息
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()          -- 事件时间
);
CREATE INDEX idx_feature_event_tenant ON feature_event(tenant_id);
CREATE INDEX idx_feature_event_created ON feature_event(created_at);
CREATE INDEX idx_feature_event_command ON feature_event(command_type);

-- -----------------------------------------------------------------------------
-- 内容片段元信息表：记录生成的内容片段描述（不含实际内容）
-- -----------------------------------------------------------------------------
CREATE TABLE content_fragment_meta (
    id              BIGSERIAL       PRIMARY KEY,                    -- 自增主键
    tenant_id       VARCHAR(64)     NOT NULL,                       -- 租户 ID
    user_id         VARCHAR(64)     NOT NULL,                       -- 用户 ID
    session_id      VARCHAR(64),                                    -- 关联会话 ID
    task_id         VARCHAR(64)     NOT NULL,                       -- 关联生成任务 ID
    content_type    VARCHAR(32)     NOT NULL,                       -- 内容类型
    relative_path   VARCHAR(1024)   NOT NULL,                       -- 文件相对路径
    start_line      INTEGER,                                        -- 片段起始行
    end_line        INTEGER,                                        -- 片段结束行
    description     VARCHAR(512),                                   -- 片段描述
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()          -- 记录时间
);
CREATE INDEX idx_content_fragment_meta_task ON content_fragment_meta(task_id);
CREATE INDEX idx_content_fragment_meta_tenant ON content_fragment_meta(tenant_id);

-- -----------------------------------------------------------------------------
-- 同步检查点表：记录各客户端的上次同步状态，支持断点续传
-- -----------------------------------------------------------------------------
CREATE TABLE sync_checkpoint (
    id              VARCHAR(128)    PRIMARY KEY,                    -- 检查点唯一标识（client_id + table_name）
    client_id       VARCHAR(128)    NOT NULL,                       -- 客户端标识
    table_name      VARCHAR(64)     NOT NULL,                       -- 同步的目标表名
    last_synced_id  BIGINT          NOT NULL DEFAULT 0,             -- 上次成功同步的最大事件 ID
    last_sync_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),         -- 上次同步时间
    status          VARCHAR(32)     NOT NULL DEFAULT 'OK',          -- 同步状态（OK/ERROR）
    error_message   TEXT                                            -- 最近一次错误信息
);
CREATE INDEX idx_sync_checkpoint_client ON sync_checkpoint(client_id);
