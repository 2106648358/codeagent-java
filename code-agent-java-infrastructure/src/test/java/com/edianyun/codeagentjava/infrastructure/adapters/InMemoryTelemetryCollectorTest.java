package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InMemoryTelemetryCollector 单元测试。
 * 验证内存中事件的记录和刷新行为。
 */
class InMemoryTelemetryCollectorTest {

    private final InMemoryTelemetryCollector collector = new InMemoryTelemetryCollector();

    @Test
    void shouldRecordAndFlushEvents() {
        TelemetryEvent event = TelemetryEvent.builder("chat")
                .sessionId("s1")
                .userId("u1")
                .tenantId("default")
                .durationMs(100)
                .success(true)
                .build();
        collector.record(event);

        List<TelemetryEvent> flushed = collector.flush();

        assertThat(flushed).hasSize(1);
        assertThat(flushed.get(0).commandType()).isEqualTo("chat");
    }

    @Test
    void shouldReturnEmptyListWhenNoEvents() {
        List<TelemetryEvent> flushed = collector.flush();
        assertThat(flushed).isEmpty();
    }

    @Test
    void shouldClearAfterFlush() {
        collector.record(TelemetryEvent.builder("test").sessionId("s").userId("u")
                .tenantId("t").durationMs(1).success(true).build());
        collector.flush();

        List<TelemetryEvent> second = collector.flush();
        assertThat(second).isEmpty();
    }

    @Test
    void shouldRecordMultipleEvents() {
        collector.record(TelemetryEvent.builder("chat").sessionId("s1").userId("u")
                .tenantId("t").durationMs(1).success(true).build());
        collector.record(TelemetryEvent.builder("generate").sessionId("s2").userId("u")
                .tenantId("t").durationMs(2).success(true).build());

        List<TelemetryEvent> flushed = collector.flush();
        assertThat(flushed).hasSize(2);
    }
}
