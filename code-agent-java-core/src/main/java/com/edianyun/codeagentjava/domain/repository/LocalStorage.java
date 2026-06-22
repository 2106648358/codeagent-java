package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.session.SessionId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 本地存储端口（出站端口），定义会话和生成任务的持久化操作。
 * 当前由 SQLite + JOOQ 实现，也可替换为内存或文件存储。
 */
public interface LocalStorage {

    /** 保存或更新会话（包括其消息列表） */
    void saveSession(Session session);

    /** 根据 ID 加载会话（含消息列表） */
    Optional<Session> loadSession(SessionId id);

    /** 保存或更新生成任务（含内容片段） */
    void saveGenerationTask(GenerationTask task);

    /** 根据 UUID 加载生成任务 */
    Optional<GenerationTask> loadGenerationTask(UUID id);

    /** 列出来自指定会话的所有消息 */
    List<Message> listMessages(SessionId id);
}
