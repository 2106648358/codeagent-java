package com.edianyun.codeagentjava.application.service;

import com.edianyun.codeagentjava.application.dto.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import com.edianyun.codeagentjava.application.dto.ChatResponse;
import com.edianyun.codeagentjava.application.port.ChatUseCase;
import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator.StreamConsumer;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;

import java.time.Instant;

/**
 * 聊天用例实现。
 * 协调 LocalStorage（加载/保存会话）、AgentOrchestrator（LLM 调用）
 * 和 TelemetryCollector（指标采集）完成一次对话流程。
 */
@Slf4j
public class ChatService implements ChatUseCase {

    private final AgentOrchestrator agentOrchestrator;
    private final StreamingAgentOrchestrator streamingAgentOrchestrator;
    private final LocalStorage localStorage;
    private final UserIdentityResolver userIdentityResolver;
    private final TelemetryCollector telemetryCollector;

    public ChatService(AgentOrchestrator agentOrchestrator, StreamingAgentOrchestrator streamingAgentOrchestrator,
                       LocalStorage localStorage, UserIdentityResolver userIdentityResolver,
                       TelemetryCollector telemetryCollector) {
        this.agentOrchestrator = agentOrchestrator;
        this.streamingAgentOrchestrator = streamingAgentOrchestrator;
        this.localStorage = localStorage;
        this.userIdentityResolver = userIdentityResolver;
        this.telemetryCollector = telemetryCollector;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        log.info("Chat request: sessionId={}, prompt={}", request.sessionId(), request.prompt());
        SessionContext ctx = prepareSession(request);

        Message userMessage = Message.user(request.prompt());
        ctx.session.addMessage(userMessage);

        Message assistantMessage = agentOrchestrator.chat(ctx.session, userMessage);
        ctx.session.addMessage(assistantMessage);

        localStorage.saveSession(ctx.session);

        recordTelemetry(ctx, start, true, null);
        log.info("Chat completed: sessionId={}, durationMs={}", ctx.sessionId.value(), System.currentTimeMillis() - start);

        return new ChatResponse(ctx.sessionId.value(), assistantMessage.content());
    }

    @Override
    public ChatResponse chatStream(ChatRequest request, StreamConsumer consumer) {
        long start = System.currentTimeMillis();
        SessionContext ctx = prepareSession(request);

        Message userMessage = Message.user(request.prompt());
        ctx.session.addMessage(userMessage);

        StringBuilder fullText = new StringBuilder();
        StreamingAgentOrchestrator.StreamConsumer wrapper = new StreamingAgentOrchestrator.StreamConsumer() {
            @Override
            public void onToken(String token) {
                fullText.append(token);
                if (consumer != null) {
                    consumer.onToken(token);
                }
            }

            @Override
            public void onComplete(String text) {
                Message assistantMessage = Message.assistant(text, null, null);
                ctx.session.addMessage(assistantMessage);
                localStorage.saveSession(ctx.session);
                recordTelemetry(ctx, start, true, null);
                if (consumer != null) {
                    consumer.onComplete(text);
                }
            }

            @Override
            public void onError(Throwable error) {
                recordTelemetry(ctx, start, false, error.getMessage());
                if (consumer != null) {
                    consumer.onError(error);
                }
            }
        };

        streamingAgentOrchestrator.chatStream(ctx.session, userMessage, wrapper);
        return new ChatResponse(ctx.sessionId.value(), fullText.toString());
    }

    private SessionContext prepareSession(ChatRequest request) {
        UserIdentity identity = userIdentityResolver.resolve();
        SessionId sessionId = request.sessionId() != null ? new SessionId(request.sessionId()) : SessionId.generate();
        Session session = localStorage.loadSession(sessionId)
                .orElseGet(() -> new Session(sessionId, resolveUserId(request, identity), identity.tenantId(), "Chat"));
        return new SessionContext(sessionId, session);
    }

    private UserId resolveUserId(ChatRequest request, UserIdentity identity) {
        return request.userId() != null ? new UserId(request.userId()) : identity.userId();
    }

    private void recordTelemetry(SessionContext ctx, long start, boolean success, String errorMessage) {
        TelemetryEvent.Builder builder = TelemetryEvent.builder("chat")
                .sessionId(ctx.sessionId.value())
                .userId(ctx.session.userId().value())
                .tenantId(ctx.session.tenantId().value())
                .timestamp(Instant.now())
                .durationMs(System.currentTimeMillis() - start)
                .success(success);
        if (errorMessage != null) {
            builder.errorMessage(errorMessage);
        }
        telemetryCollector.record(builder.build());
    }

    private record SessionContext(SessionId sessionId, Session session) {
    }
}
