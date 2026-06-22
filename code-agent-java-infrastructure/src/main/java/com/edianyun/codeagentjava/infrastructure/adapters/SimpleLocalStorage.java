package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存本地存储适配器（用于测试或轻量场景）。
 * 使用 ConcurrentHashMap 在内存中保存会话和生成任务数据。
 */
public class SimpleLocalStorage implements LocalStorage {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, GenerationTask> tasks = new ConcurrentHashMap<>();

    @Override
    public void saveSession(Session session) {
        sessions.put(session.id().value(), session);
    }

    @Override
    public Optional<Session> loadSession(SessionId id) {
        return Optional.ofNullable(sessions.get(id.value()));
    }

    @Override
    public void saveGenerationTask(GenerationTask task) {
        tasks.put(task.id(), task);
    }

    @Override
    public Optional<GenerationTask> loadGenerationTask(UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }

    @Override
    public List<Message> listMessages(SessionId id) {
        return loadSession(id).map(Session::messages).orElseGet(ArrayList::new);
    }
}
