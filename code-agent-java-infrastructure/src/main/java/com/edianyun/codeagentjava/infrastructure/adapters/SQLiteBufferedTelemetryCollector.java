package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import org.jooq.DSLContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.edianyun.codeagentjava.infrastructure.db.jooq.tables.TelemetryEvents.TELEMETRY_EVENTS;

/**
 * SQLite 缓冲遥测采集器适配器。
 * 事件先缓冲在内存中，flush 时批量写入 SQLite 的 telemetry_events 表，
 * 确保离线时事件不丢失，后续由 RemoteSyncService 同步到远程 PostgreSQL。
 */
public class SQLiteBufferedTelemetryCollector implements TelemetryCollector {

    private final DSLContext dsl;
    private final List<TelemetryEvent> buffer = new ArrayList<>();

    public SQLiteBufferedTelemetryCollector(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public synchronized void record(TelemetryEvent event) {
        buffer.add(event);
    }

    @Override
    public synchronized List<TelemetryEvent> flush() {
        if (buffer.isEmpty()) {
            return List.of();
        }
        List<TelemetryEvent> copy = new ArrayList<>(buffer);
        for (TelemetryEvent event : copy) {
            dsl.insertInto(TELEMETRY_EVENTS)
                    .set(TELEMETRY_EVENTS.COMMAND_TYPE, event.commandType())
                    .set(TELEMETRY_EVENTS.SESSION_ID, event.sessionId())
                    .set(TELEMETRY_EVENTS.USER_ID, event.userId())
                    .set(TELEMETRY_EVENTS.TENANT_ID, event.tenantId())
                    .set(TELEMETRY_EVENTS.MODEL_ID, event.modelId() != null ? event.modelId().value() : null)
                    .set(TELEMETRY_EVENTS.DURATION_MS, event.durationMs())
                    .set(TELEMETRY_EVENTS.CONTENT_TYPE, event.contentType() != null ? event.contentType().name() : null)
                    .set(TELEMETRY_EVENTS.FILE_EXTENSION, event.fileExtension())
                    .set(TELEMETRY_EVENTS.FILE_COUNT, event.fileCount())
                    .set(TELEMETRY_EVENTS.SUCCESS, event.success() ? 1 : 0)
                    .set(TELEMETRY_EVENTS.ERROR_MESSAGE, event.errorMessage())
                    .set(TELEMETRY_EVENTS.TIMESTAMP, event.timestamp().toEpochMilli())
                    .execute();
        }
        buffer.clear();
        return Collections.unmodifiableList(copy);
    }
}
