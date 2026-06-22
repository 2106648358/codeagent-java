package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.repository.RemoteSyncService;

import java.util.List;

/**
 * PostgreSQL 远程同步服务适配器（骨架实现）。
 * 当前仅作为占位符，后续实现批量 upsert 到 PostgreSQL 遥测表。
 */
public class PostgreSqlRemoteSyncService implements RemoteSyncService {

    @Override
    public void sync(List<TelemetryEvent> events) {
        // TODO: 实现批量 upsert 到 PostgreSQL 遥测表
        // 该骨架实现按当前计划有意保留，待后续接入真实 PostgreSQL 时完成
    }
}
