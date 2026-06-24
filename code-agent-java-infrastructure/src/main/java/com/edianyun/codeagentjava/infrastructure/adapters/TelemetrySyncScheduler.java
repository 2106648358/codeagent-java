package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.repository.RemoteSyncService;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 遥测后台同步调度器。
 * <p>
 * 同步策略（双重触发）：
 * 1. 定时触发：每隔配置的间隔时间（默认 30 秒）自动执行一次同步
 * 2. 阈值触发（后续实现）：当缓冲区事件数达到 batch-size 时立即同步
 * <p>
 * 执行流程：
 * 1. 调用 TelemetryCollector.flush() 将内存缓冲区事件写入本地 SQLite，
 *    并获取本次刷出的事件列表
 * 2. 调用 RemoteSyncService.sync(events) 将事件批量 upsert 到远程 PostgreSQL
 * 3. 若 PostgreSQL 未配置，sync() 内部静默跳过
 */
@Slf4j
@Component
public class TelemetrySyncScheduler {

    private final TelemetryCollector telemetryCollector;
    private final RemoteSyncService remoteSyncService;

    public TelemetrySyncScheduler(TelemetryCollector telemetryCollector, RemoteSyncService remoteSyncService) {
        this.telemetryCollector = telemetryCollector;
        this.remoteSyncService = remoteSyncService;
    }

    /**
     * 定时同步任务。
     * 执行间隔由 codeagent.telemetry.flush-interval-seconds 配置控制（默认 30 秒）。
     * fixedDelayString 确保每次执行完成后间隔固定时间再触发下一次，
     * 避免任务堆积。
     */
    @Scheduled(fixedDelayString = "${codeagent.telemetry.flush-interval-seconds:30}000")
    public void scheduledSync() {
        log.debug("Running scheduled telemetry sync");
        List<TelemetryEvent> events = telemetryCollector.flush();
        if (!events.isEmpty()) {
            log.info("Flushed {} telemetry event(s), syncing to remote", events.size());
            remoteSyncService.sync(events);
        }
    }
}
