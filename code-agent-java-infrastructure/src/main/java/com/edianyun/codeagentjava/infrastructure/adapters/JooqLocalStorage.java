package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.content.ModelId;
import com.edianyun.codeagentjava.domain.model.content.TokenUsage;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.message.Role;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.infrastructure.db.jooq.tables.records.ContentFragmentsRecord;
import com.edianyun.codeagentjava.infrastructure.db.jooq.tables.records.GenerationTasksRecord;
import com.edianyun.codeagentjava.infrastructure.db.jooq.tables.records.MessagesRecord;
import com.edianyun.codeagentjava.infrastructure.db.jooq.tables.records.SessionsRecord;
import org.jooq.DSLContext;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.edianyun.codeagentjava.infrastructure.db.jooq.tables.ContentFragments.CONTENT_FRAGMENTS;
import static com.edianyun.codeagentjava.infrastructure.db.jooq.tables.GenerationTasks.GENERATION_TASKS;
import static com.edianyun.codeagentjava.infrastructure.db.jooq.tables.Messages.MESSAGES;
import static com.edianyun.codeagentjava.infrastructure.db.jooq.tables.Sessions.SESSIONS;

/**
 * JOOQ + SQLite 本地存储适配器，实现 LocalStorage 端口。
 * 将领域实体（Session、GenerationTask、Message）持久化到 SQLite 数据库。
 * 使用 JOOQ 生成的 DSL 类操作数据库，支持 upsert 语义。
 */
public class JooqLocalStorage implements LocalStorage {

    private final DSLContext dsl;

    public JooqLocalStorage(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public void saveSession(Session session) {
        dsl.insertInto(SESSIONS)
                .set(SESSIONS.ID, session.id().value())
                .set(SESSIONS.USER_ID, session.userId().value())
                .set(SESSIONS.TENANT_ID, session.tenantId().value())
                .set(SESSIONS.TITLE, session.title())
                .set(SESSIONS.CREATED_AT, session.createdAt().toEpochMilli())
                .set(SESSIONS.UPDATED_AT, session.updatedAt().toEpochMilli())
                .onConflict(SESSIONS.ID)
                .doUpdate()
                .set(SESSIONS.TITLE, session.title())
                .set(SESSIONS.UPDATED_AT, session.updatedAt().toEpochMilli())
                .execute();

        dsl.deleteFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(session.id().value()))
                .execute();

        for (Message message : session.messages()) {
            TokenUsage tokenUsage = message.tokenUsage();
            Integer promptTokens = tokenUsage != null ? tokenUsage.inputTokens() : null;
            Integer completionTokens = tokenUsage != null ? tokenUsage.outputTokens() : null;
            Integer totalTokens = tokenUsage != null ? tokenUsage.totalTokens() : null;
            dsl.insertInto(MESSAGES)
                    .set(MESSAGES.SESSION_ID, session.id().value())
                    .set(MESSAGES.ROLE, message.role().name())
                    .set(MESSAGES.CONTENT, message.content())
                    .set(MESSAGES.MODEL_ID, message.modelId() != null ? message.modelId().value() : null)
                    .set(MESSAGES.PROMPT_TOKENS, promptTokens)
                    .set(MESSAGES.COMPLETION_TOKENS, completionTokens)
                    .set(MESSAGES.TOTAL_TOKENS, totalTokens)
                    .set(MESSAGES.TIMESTAMP, message.timestamp().toEpochMilli())
                    .execute();
        }
    }

    @Override
    public Optional<Session> loadSession(SessionId id) {
        SessionsRecord record = dsl.selectFrom(SESSIONS)
                .where(SESSIONS.ID.eq(id.value()))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }

        Session session = new Session(
                new SessionId(record.getId()),
                new UserId(record.getUserId()),
                new TenantId(record.getTenantId()),
                record.getTitle(),
                Instant.ofEpochMilli(record.getCreatedAt()),
                Instant.ofEpochMilli(record.getUpdatedAt())
        );

        List<MessagesRecord> messageRecords = dsl.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(id.value()))
                .fetch();
        for (MessagesRecord msg : messageRecords) {
            session.addMessage(toMessage(msg));
        }
        return Optional.of(session);
    }

    @Override
    public void saveGenerationTask(GenerationTask task) {
        dsl.insertInto(GENERATION_TASKS)
                .set(GENERATION_TASKS.ID, task.id().toString())
                .set(GENERATION_TASKS.SESSION_ID, task.sessionId().value())
                .set(GENERATION_TASKS.REQUIREMENTS, task.requirements())
                .set(GENERATION_TASKS.CONTENT_TYPE, task.contentType().name())
                .set(GENERATION_TASKS.STATUS, task.status())
                .set(GENERATION_TASKS.CREATED_AT, task.createdAt().toEpochMilli())
                .set(GENERATION_TASKS.COMPLETED_AT, task.completedAt() != null ? task.completedAt().toEpochMilli() : null)
                .onConflict(GENERATION_TASKS.ID)
                .doUpdate()
                .set(GENERATION_TASKS.REQUIREMENTS, task.requirements())
                .set(GENERATION_TASKS.CONTENT_TYPE, task.contentType().name())
                .set(GENERATION_TASKS.STATUS, task.status())
                .set(GENERATION_TASKS.COMPLETED_AT, task.completedAt() != null ? task.completedAt().toEpochMilli() : null)
                .execute();

        dsl.deleteFrom(CONTENT_FRAGMENTS)
                .where(CONTENT_FRAGMENTS.TASK_ID.eq(task.id().toString()))
                .execute();

        for (ContentFragment fragment : task.fragments()) {
            dsl.insertInto(CONTENT_FRAGMENTS)
                    .set(CONTENT_FRAGMENTS.TASK_ID, task.id().toString())
                    .set(CONTENT_FRAGMENTS.CONTENT_TYPE, fragment.contentType().name())
                    .set(CONTENT_FRAGMENTS.RELATIVE_PATH, fragment.path().value())
                    .set(CONTENT_FRAGMENTS.CONTENT, fragment.content())
                    .set(CONTENT_FRAGMENTS.DESCRIPTION, fragment.description())
                    .execute();
        }
    }

    @Override
    public Optional<GenerationTask> loadGenerationTask(UUID id) {
        GenerationTasksRecord record = dsl.selectFrom(GENERATION_TASKS)
                .where(GENERATION_TASKS.ID.eq(id.toString()))
                .fetchOne();
        if (record == null) {
            return Optional.empty();
        }

        GenerationTask task = new GenerationTask(
                UUID.fromString(record.getId()),
                new SessionId(record.getSessionId()),
                record.getRequirements(),
                ContentType.valueOf(record.getContentType()),
                record.getStatus(),
                Instant.ofEpochMilli(record.getCreatedAt()),
                record.getCompletedAt() != null ? Instant.ofEpochMilli(record.getCompletedAt()) : null
        );

        List<ContentFragmentsRecord> fragmentRecords = dsl.selectFrom(CONTENT_FRAGMENTS)
                .where(CONTENT_FRAGMENTS.TASK_ID.eq(id.toString()))
                .fetch();
        for (ContentFragmentsRecord fragment : fragmentRecords) {
            task.addFragment(toFragment(fragment));
        }
        return Optional.of(task);
    }

    @Override
    public List<Message> listMessages(SessionId id) {
        return dsl.selectFrom(MESSAGES)
                .where(MESSAGES.SESSION_ID.eq(id.value()))
                .fetch()
                .stream()
                .map(this::toMessage)
                .collect(Collectors.toList());
    }

    private Message toMessage(MessagesRecord record) {
        ModelId modelId = record.getModelId() != null ? new ModelId(record.getModelId()) : null;
        TokenUsage tokenUsage = null;
        if (record.getPromptTokens() != null && record.getCompletionTokens() != null) {
            tokenUsage = new TokenUsage(record.getPromptTokens(), record.getCompletionTokens());
        }
        return new Message(
                Role.valueOf(record.getRole()),
                record.getContent(),
                modelId,
                tokenUsage,
                Instant.ofEpochMilli(record.getTimestamp())
        );
    }

    private ContentFragment toFragment(ContentFragmentsRecord record) {
        ContentFragment fragment = ContentFragment.of(
                new RelativePath(record.getRelativePath()),
                record.getContent(),
                ContentType.valueOf(record.getContentType())
        );
        if (record.getDescription() != null) {
            fragment = fragment.withDescription(record.getDescription());
        }
        return fragment;
    }
}
