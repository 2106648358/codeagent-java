package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 内存遥测采集器适配器（备用/遗留版本）。
 * 仅将事件缓存在内存列表中，适用于测试场景。
 */
public class InMemoryTelemetryCollector implements TelemetryCollector {

    private final List<TelemetryEvent> events = new ArrayList<>();

    @Override
    public synchronized void record(TelemetryEvent event) {
        events.add(event);
    }

    @Override
    public synchronized List<TelemetryEvent> flush() {
        if (events.isEmpty()) {
            return List.of();
        }
        List<TelemetryEvent> copy = new ArrayList<>(events);
        events.clear();
        return Collections.unmodifiableList(copy);
    }
}
