package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;

import java.util.List;

/**
 * 远程同步服务端口（出站端口），将本地遥测事件批量同步到远程服务器。
 * 当前为骨架实现，后续接入真实 PostgreSQL 后启用。
 */
public interface RemoteSyncService {

    /** 批量同步遥测事件到远程仓库 */
    void sync(List<TelemetryEvent> events);
}
