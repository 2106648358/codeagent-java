package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.repository.RemoteSyncService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

/**
 * PostgreSQL 远程同步服务适配器。
 * 将本地 SQLite 中缓冲的遥测事件批量同步到 PostgreSQL 远程遥测表。
 * <p>
 * 同步策略：
 * - 租户和开发者信息使用 INSERT ... ON CONFLICT DO NOTHING（幂等）
 * - 功能事件和 LLM 用量使用批量 INSERT
 * - 当 PostgreSQL DataSource 未配置时（postgresDSLContext == null），所有同步操作静默跳过
 * <p>
 * 远程遥测表结构（db/migration-pg/V1__postgres_telemetry.sql）：
 * - tenant：租户信息
 * - developer：开发者信息
 * - feature_event：功能使用事件
 * - llm_usage：LLM 调用用量
 * - session_metrics：会话聚合指标（后续实现）
 * - content_fragment_meta：内容片段元信息（后续实现）
 * - sync_checkpoint：同步检查点（后续实现）
 */
@Slf4j
public class PostgreSqlRemoteSyncService implements RemoteSyncService {

    /** 本地 SQLite DSLContext，用于读取待同步事件 */
    private final DSLContext localDSL;

    /** 远程 PostgreSQL DSLContext，未配置时为 null */
    private final DSLContext postgresDSL;

    // ---- PostgreSQL 遥测表引用（相当于 JOOQ 代码生成产物） ----

    private static final Table<Record> TENANT = table("tenant");
    private static final Field<String> TENANT_ID = field("id", String.class);
    private static final Field<String> TENANT_NAME = field("name", String.class);
    private static final Field<Timestamp> TENANT_CREATED_AT = field("created_at", Timestamp.class);
    private static final Field<Timestamp> TENANT_UPDATED_AT = field("updated_at", Timestamp.class);

    private static final Table<Record> DEVELOPER = table("developer");
    private static final Field<String> DEV_ID = field("id", String.class);
    private static final Field<String> DEV_TENANT_ID = field("tenant_id", String.class);
    private static final Field<String> DEV_NAME = field("name", String.class);
    private static final Field<Timestamp> DEV_CREATED_AT = field("created_at", Timestamp.class);
    private static final Field<Timestamp> DEV_LAST_ACTIVE_AT = field("last_active_at", Timestamp.class);

    private static final Table<Record> FEATURE_EVENT = table("feature_event");
    private static final Field<String> FE_TENANT_ID = field("tenant_id", String.class);
    private static final Field<String> FE_USER_ID = field("user_id", String.class);
    private static final Field<String> FE_SESSION_ID = field("session_id", String.class);
    private static final Field<String> FE_COMMAND_TYPE = field("command_type", String.class);
    private static final Field<String> FE_CONTENT_TYPE = field("content_type", String.class);
    private static final Field<String> FE_FILE_EXTENSION = field("file_extension", String.class);
    private static final Field<Integer> FE_FILE_COUNT = field("file_count", Integer.class);
    private static final Field<Long> FE_DURATION_MS = field("duration_ms", Long.class);
    private static final Field<Boolean> FE_SUCCESS = field("success", Boolean.class);
    private static final Field<String> FE_ERROR_MESSAGE = field("error_message", String.class);
    private static final Field<Timestamp> FE_CREATED_AT = field("created_at", Timestamp.class);

    private static final Table<Record> LLM_USAGE = table("llm_usage");
    private static final Field<String> LU_TENANT_ID = field("tenant_id", String.class);
    private static final Field<String> LU_USER_ID = field("user_id", String.class);
    private static final Field<String> LU_SESSION_ID = field("session_id", String.class);
    private static final Field<String> LU_MODEL_ID = field("model_id", String.class);
    private static final Field<Integer> LU_PROMPT_TOKENS = field("prompt_tokens", Integer.class);
    private static final Field<Integer> LU_COMPLETION_TOKENS = field("completion_tokens", Integer.class);
    private static final Field<Integer> LU_TOTAL_TOKENS = field("total_tokens", Integer.class);
    private static final Field<Long> LU_DURATION_MS = field("duration_ms", Long.class);
    private static final Field<Timestamp> LU_CREATED_AT = field("created_at", Timestamp.class);

    public PostgreSqlRemoteSyncService(DSLContext localDSL, DSLContext postgresDSL) {
        this.localDSL = localDSL;
        this.postgresDSL = postgresDSL;
    }

    /**
     * 批量同步遥测事件到 PostgreSQL。
     * 对每个事件执行：
     * 1. 幂等写入租户信息
     * 2. 幂等写入开发者信息
     * 3. 插入功能事件记录
     * 4. 如果有模型信息，插入 LLM 用量记录
     * <p>
     * 当 PostgreSQL 未配置时，静默跳过所有操作。
     *
     * @param events 待同步的遥测事件列表
     */
    @Override
    public void sync(List<TelemetryEvent> events) {
        if (postgresDSL == null || events == null || events.isEmpty()) {
            if (events != null && !events.isEmpty()) {
                log.debug("PostgreSQL not configured, skipping sync of {} event(s)", events.size());
            }
            return;
        }
        log.info("Syncing {} telemetry event(s) to PostgreSQL", events.size());

        Timestamp now = Timestamp.from(Instant.now());
        for (TelemetryEvent event : events) {
            String tenantId = nullSafe(event.tenantId(), "default");
            String userId = nullSafe(event.userId(), "anonymous");
            String sessionId = event.sessionId();

            // 幂等写入租户（INSERT ... ON CONFLICT DO NOTHING），确保引用完整性
            postgresDSL.insertInto(TENANT)
                    .set(TENANT_ID, tenantId)
                    .set(TENANT_NAME, tenantId)
                    .set(TENANT_CREATED_AT, now)
                    .set(TENANT_UPDATED_AT, now)
                    .onConflict(TENANT_ID)
                    .doUpdate()
                    .set(TENANT_UPDATED_AT, now)
                    .execute();

            // 幂等写入开发者（更新最后活跃时间）
            postgresDSL.insertInto(DEVELOPER)
                    .set(DEV_ID, userId)
                    .set(DEV_TENANT_ID, tenantId)
                    .set(DEV_CREATED_AT, now)
                    .set(DEV_LAST_ACTIVE_AT, now)
                    .onConflict(DEV_ID)
                    .doUpdate()
                    .set(DEV_LAST_ACTIVE_AT, now)
                    .execute();

            // 插入功能事件
            postgresDSL.insertInto(FEATURE_EVENT)
                    .set(FE_TENANT_ID, tenantId)
                    .set(FE_USER_ID, userId)
                    .set(FE_SESSION_ID, sessionId)
                    .set(FE_COMMAND_TYPE, event.commandType())
                    .set(FE_CONTENT_TYPE, event.contentType() != null ? event.contentType().name() : null)
                    .set(FE_FILE_EXTENSION, event.fileExtension())
                    .set(FE_FILE_COUNT, event.fileCount() > 0 ? event.fileCount() : null)
                    .set(FE_DURATION_MS, event.durationMs())
                    .set(FE_SUCCESS, event.success())
                    .set(FE_ERROR_MESSAGE, event.errorMessage())
                    .set(FE_CREATED_AT, Timestamp.from(event.timestamp()))
                    .execute();

            // 如果有模型信息，插入 LLM 用量记录
            // 如果有模型信息，插入 LLM 用量记录
            if (event.modelId() != null) {
                log.debug("Syncing LLM usage: model={}, sessionId={}", event.modelId().value(), sessionId);
                postgresDSL.insertInto(LLM_USAGE)
                        .set(LU_TENANT_ID, tenantId)
                        .set(LU_USER_ID, userId)
                        .set(LU_SESSION_ID, sessionId)
                        .set(LU_MODEL_ID, event.modelId().value())
                        .set(LU_DURATION_MS, event.durationMs())
                        .set(LU_CREATED_AT, Timestamp.from(event.timestamp()))
                        .execute();
            }
        }
    }

    private static String nullSafe(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }
}
