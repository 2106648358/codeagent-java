package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;

import java.util.List;

/**
 * 遥测采集器端口（出站端口），用于记录和提交遥测事件。
 * 事件先在内存中缓冲，定期 flush 到 SQLite 或远程 PostgreSQL。
 */
public interface TelemetryCollector {

    /** 记录一条遥测事件到缓冲区 */
    void record(TelemetryEvent event);

    /** 刷新缓冲区，返回已记录的事件列表并清空缓冲区 */
    List<TelemetryEvent> flush();
}
