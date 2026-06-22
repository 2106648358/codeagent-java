package com.edianyun.codeagentjava;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQLite 持久化集成测试。
 * 使用临时 SQLite 数据库文件测试 JooqLocalStorage 和 SQLiteBufferedTelemetryCollector
 * 的读写、更新和刷新操作。
 */
@SpringBootTest
class SqlitePersistenceIntegrationTest {

    @DynamicPropertySource
    static void sqlitePath(DynamicPropertyRegistry registry) {
        registry.add("codeagent.storage.sqlite.path", () -> {
            try {
                return Files.createTempFile("codeagent-test", ".db").toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Autowired
    private LocalStorage localStorage;

    @Autowired
    private TelemetryCollector telemetryCollector;

    @Test
    void shouldSaveAndLoadSessionWithMessages() {
        SessionId sessionId = SessionId.generate();
        Session session = new Session(sessionId, UserId.anonymous(), TenantId.defaultTenant(), "Test");
        session.addMessage(Message.user("Hello"));
        localStorage.saveSession(session);

        Optional<Session> loaded = localStorage.loadSession(sessionId);
        assertTrue(loaded.isPresent());
        assertEquals("Test", loaded.get().title());
        assertEquals(1, loaded.get().messages().size());
        assertEquals("Hello", loaded.get().messages().get(0).content());
    }

    @Test
    void shouldSaveAndLoadGenerationTaskWithFragments() {
        SessionId sessionId = SessionId.generate();
        GenerationTask task = new GenerationTask(UUID.randomUUID(), sessionId, "Create a hello world", ContentType.CODE);
        task.addFragment(ContentFragment.of(new RelativePath("Hello.java"), "public class Hello {}", ContentType.CODE));
        task.complete();
        localStorage.saveGenerationTask(task);

        Optional<GenerationTask> loaded = localStorage.loadGenerationTask(task.id());
        assertTrue(loaded.isPresent());
        assertEquals("COMPLETED", loaded.get().status());
        assertEquals(1, loaded.get().fragments().size());
        assertEquals("Hello.java", loaded.get().fragments().get(0).path().value());
    }

    @Test
    void shouldRecordAndFlushTelemetryEvents() {
        TelemetryEvent event = TelemetryEvent.builder("chat")
                .sessionId("session-1")
                .userId("anonymous")
                .tenantId("default")
                .durationMs(100)
                .success(true)
                .build();
        telemetryCollector.record(event);
        List<TelemetryEvent> flushed = telemetryCollector.flush();
        assertEquals(1, flushed.size());
        assertEquals("chat", flushed.get(0).commandType());
        assertEquals("session-1", flushed.get(0).sessionId());
    }
}
