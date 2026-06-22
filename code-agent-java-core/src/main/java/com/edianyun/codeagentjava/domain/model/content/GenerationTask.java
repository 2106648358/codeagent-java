package com.edianyun.codeagentjava.domain.model.content;

import com.edianyun.codeagentjava.domain.model.session.SessionId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 生成任务实体，表示一次内容生成请求的完整生命周期。
 * 从需求输入 -> LLM 调用 -> 内容片段产出 -> 完成/失败，
 * 包含状态流转和关联的内容片段列表。
 */
public class GenerationTask {

    private final UUID id;
    private final SessionId sessionId;
    private String requirements;
    private ContentType contentType;
    private String status;
    private final Instant createdAt;
    private Instant completedAt;
    private final List<ContentFragment> fragments;

    public GenerationTask(UUID id, SessionId sessionId, String requirements, ContentType contentType) {
        this(id, sessionId, requirements, contentType, "PENDING", Instant.now(), null);
    }

    public GenerationTask(UUID id, SessionId sessionId, String requirements, ContentType contentType,
                          String status, Instant createdAt, Instant completedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.requirements = requirements;
        this.contentType = contentType;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.fragments = new ArrayList<>();
    }

    public UUID id() { return id; }

    public SessionId sessionId() { return sessionId; }

    public String requirements() { return requirements; }

    public void requirements(String requirements) { this.requirements = requirements; }

    public ContentType contentType() { return contentType; }

    public void contentType(ContentType contentType) { this.contentType = contentType; }

    public String status() { return status; }

    public void status(String status) { this.status = status; }

    /** 返回不可修改的内容片段列表 */
    public List<ContentFragment> fragments() { return Collections.unmodifiableList(fragments); }

    public void addFragment(ContentFragment fragment) { this.fragments.add(fragment); }

    public void addFragments(List<ContentFragment> fragments) { this.fragments.addAll(fragments); }

    public Instant createdAt() { return createdAt; }

    public Instant completedAt() { return completedAt; }

    /** 将任务标记为已完成 */
    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }

    /** 将任务标记为失败 */
    public void fail() {
        this.status = "FAILED";
        this.completedAt = Instant.now();
    }
}
