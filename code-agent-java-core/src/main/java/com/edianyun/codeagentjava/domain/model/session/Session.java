package com.edianyun.codeagentjava.domain.model.session;

import com.edianyun.codeagentjava.domain.model.message.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话实体，代表一次 AI 对话或工作单元。
 * 一个 Session 包含多个 Message 组成完整的对话历史，
 * 由 sessionId + userId + tenantId 共同确定归属。
 */
public class Session {

    private final SessionId id;
    private final UserId userId;
    private final TenantId tenantId;
    private String title;
    private final List<Message> messages;
    private final Instant createdAt;
    private Instant updatedAt;

    public Session(SessionId id, UserId userId, TenantId tenantId, String title) {
        this(id, userId, tenantId, title, Instant.now(), Instant.now());
    }

    public Session(SessionId id, UserId userId, TenantId tenantId, String title, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.tenantId = tenantId;
        this.title = title;
        this.messages = new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public SessionId id() { return id; }

    public UserId userId() { return userId; }

    public TenantId tenantId() { return tenantId; }

    public String title() { return title; }

    public void title(String title) { this.title = title; }

    /** 返回不可修改的消息列表，防止外部破坏对话历史 */
    public List<Message> messages() { return Collections.unmodifiableList(messages); }

    /** 添加一条消息，并自动更新会话时间戳 */
    public void addMessage(Message message) {
        messages.add(message);
        this.updatedAt = Instant.now();
    }

    public Instant createdAt() { return createdAt; }

    public Instant updatedAt() { return updatedAt; }
}
