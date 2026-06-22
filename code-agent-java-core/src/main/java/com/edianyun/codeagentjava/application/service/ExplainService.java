package com.edianyun.codeagentjava.application.service;

import com.edianyun.codeagentjava.application.dto.ExplainRequest;
import com.edianyun.codeagentjava.application.dto.ExplainResponse;
import com.edianyun.codeagentjava.application.port.ExplainUseCase;
import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator.StreamConsumer;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

/**
 * 解释用例实现。
 * 扫描工作区获取上下文，调用 LLM 对指定目标进行解释，
 * 并记录遥测事件。
 */
public class ExplainService implements ExplainUseCase {

    private final AgentOrchestrator agentOrchestrator;
    private final StreamingAgentOrchestrator streamingAgentOrchestrator;
    private final FileRepository fileRepository;
    private final UserIdentityResolver userIdentityResolver;
    private final TelemetryCollector telemetryCollector;

    public ExplainService(AgentOrchestrator agentOrchestrator, StreamingAgentOrchestrator streamingAgentOrchestrator,
                          FileRepository fileRepository, UserIdentityResolver userIdentityResolver,
                          TelemetryCollector telemetryCollector) {
        this.agentOrchestrator = agentOrchestrator;
        this.streamingAgentOrchestrator = streamingAgentOrchestrator;
        this.fileRepository = fileRepository;
        this.userIdentityResolver = userIdentityResolver;
        this.telemetryCollector = telemetryCollector;
    }

    @Override
    public ExplainResponse explain(ExplainRequest request) {
        long start = System.currentTimeMillis();
        ExplainContext ctx = prepareContext(request);

        String explanation = agentOrchestrator.explain(request.target(), ctx.context);

        recordTelemetry(ctx, start, true, null);

        return new ExplainResponse(request.target(), explanation);
    }

    @Override
    public ExplainResponse explainStream(ExplainRequest request, StreamConsumer consumer) {
        long start = System.currentTimeMillis();
        ExplainContext ctx = prepareContext(request);

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

        streamingAgentOrchestrator.explainStream(request.target(), ctx.context, wrapper);
        return new ExplainResponse(request.target(), fullText.toString());
    }

    private ExplainContext prepareContext(ExplainRequest request) {
        UserIdentity identity = userIdentityResolver.resolve();
        SessionId sessionId = request.sessionId() != null ? new SessionId(request.sessionId()) : SessionId.generate();
        Path workingDir = Paths.get(".").toAbsolutePath().normalize();
        WorkspaceContext context = fileRepository.scan(workingDir, FileRepository.ScanOptions.defaults());
        return new ExplainContext(sessionId, context, identity);
    }

    private UserId resolveUserId(ExplainRequest request, UserIdentity identity) {
        return request.userId() != null ? new UserId(request.userId()) : identity.userId();
    }

    private void recordTelemetry(ExplainContext ctx, long start, boolean success, String errorMessage) {
        TelemetryEvent.Builder builder = TelemetryEvent.builder("explain")
                .sessionId(ctx.sessionId.value())
                .userId(ctx.userId.value())
                .tenantId(ctx.tenantId.value())
                .timestamp(Instant.now())
                .durationMs(System.currentTimeMillis() - start)
                .success(success);
        if (errorMessage != null) {
            builder.errorMessage(errorMessage);
        }
        telemetryCollector.record(builder.build());
    }

    private record ExplainContext(SessionId sessionId, WorkspaceContext context, UserId userId, TenantId tenantId) {
        private ExplainContext {
        }

        private ExplainContext(SessionId sessionId, WorkspaceContext context, UserIdentity identity) {
            this(sessionId, context, identity.userId(), identity.tenantId());
        }
    }
}
