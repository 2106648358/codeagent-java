package com.edianyun.codeagentjava.application.service;

import com.edianyun.codeagentjava.application.dto.GenerateRequest;
import com.edianyun.codeagentjava.application.dto.GenerateResponse;
import com.edianyun.codeagentjava.application.port.GenerateUseCase;
import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.telemetry.TelemetryEvent;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator.StreamConsumer;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;
import com.edianyun.codeagentjava.domain.service.ContentFragmentExtractor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 生成用例实现。
 * 协调工作区扫描、LLM 调用、内容提取和持久化，
 * 完成 "根据需求生成文件内容" 的完整流程。
 */
public class GenerateService implements GenerateUseCase {

    private final AgentOrchestrator agentOrchestrator;
    private final StreamingAgentOrchestrator streamingAgentOrchestrator;
    private final FileRepository fileRepository;
    private final LocalStorage localStorage;
    private final UserIdentityResolver userIdentityResolver;
    private final TelemetryCollector telemetryCollector;
    private final ContentFragmentExtractor contentFragmentExtractor;

    public GenerateService(AgentOrchestrator agentOrchestrator, StreamingAgentOrchestrator streamingAgentOrchestrator,
                           FileRepository fileRepository, LocalStorage localStorage,
                           UserIdentityResolver userIdentityResolver, TelemetryCollector telemetryCollector,
                           ContentFragmentExtractor contentFragmentExtractor) {
        this.agentOrchestrator = agentOrchestrator;
        this.streamingAgentOrchestrator = streamingAgentOrchestrator;
        this.fileRepository = fileRepository;
        this.localStorage = localStorage;
        this.userIdentityResolver = userIdentityResolver;
        this.telemetryCollector = telemetryCollector;
        this.contentFragmentExtractor = contentFragmentExtractor;
    }

    @Override
    public GenerateResponse generate(GenerateRequest request) {
        long start = System.currentTimeMillis();
        GenerateContext ctx = prepareContext(request);

        GenerationTask task = new GenerationTask(UUID.randomUUID(), ctx.sessionId, request.requirements(), ctx.contentType);
        task = agentOrchestrator.generate(task, ctx.context);
        task.complete();

        localStorage.saveGenerationTask(task);

        recordTelemetry(ctx, start, true, null, task.fragments().size());

        return new GenerateResponse(task.id().toString(), ctx.sessionId.value(), task.fragments());
    }

    @Override
    public GenerateResponse generateStream(GenerateRequest request, StreamConsumer consumer) {
        long start = System.currentTimeMillis();
        GenerateContext ctx = prepareContext(request);

        GenerationTask task = new GenerationTask(UUID.randomUUID(), ctx.sessionId, request.requirements(), ctx.contentType);
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
                List<ContentFragment> fragments = contentFragmentExtractor.extract(text, ctx.contentType);
                task.addFragments(fragments);
                task.complete();
                localStorage.saveGenerationTask(task);
                recordTelemetry(ctx, start, true, null, fragments.size());
                if (consumer != null) {
                    consumer.onComplete(text);
                }
            }

            @Override
            public void onError(Throwable error) {
                task.fail();
                localStorage.saveGenerationTask(task);
                recordTelemetry(ctx, start, false, error.getMessage(), 0);
                if (consumer != null) {
                    consumer.onError(error);
                }
            }
        };

        streamingAgentOrchestrator.generateStream(task, ctx.context, wrapper);
        return new GenerateResponse(task.id().toString(), ctx.sessionId.value(), task.fragments());
    }

    private GenerateContext prepareContext(GenerateRequest request) {
        UserIdentity identity = userIdentityResolver.resolve();
        SessionId sessionId = request.sessionId() != null ? new SessionId(request.sessionId()) : SessionId.generate();
        Path workingDir = Paths.get(".").toAbsolutePath().normalize();

        WorkspaceContext scanned = fileRepository.scan(workingDir, FileRepository.ScanOptions.defaults());
        ContentType contentType = request.contentType() != null ? request.contentType() : ContentType.CODE;

        List<RelativePath> selectedFiles = request.targetFiles() != null
                ? request.targetFiles().stream().map(RelativePath::new).collect(Collectors.toList())
                : List.of();
        WorkspaceContext context = new WorkspaceContext(workingDir, scanned.files(), selectedFiles, contentType, null);

        return new GenerateContext(sessionId, context, contentType, identity);
    }

    private UserId resolveUserId(GenerateRequest request, UserIdentity identity) {
        return request.userId() != null ? new UserId(request.userId()) : identity.userId();
    }

    private void recordTelemetry(GenerateContext ctx, long start, boolean success, String errorMessage, int fragmentCount) {
        TelemetryEvent.Builder builder = TelemetryEvent.builder("generate")
                .sessionId(ctx.sessionId.value())
                .userId(ctx.userId.value())
                .tenantId(ctx.tenantId.value())
                .contentType(ctx.contentType)
                .fileCount(ctx.context.selectedFiles().size())
                .timestamp(Instant.now())
                .durationMs(System.currentTimeMillis() - start)
                .success(success);
        if (errorMessage != null) {
            builder.errorMessage(errorMessage);
        }
        telemetryCollector.record(builder.build());
    }

    private record GenerateContext(SessionId sessionId, WorkspaceContext context, ContentType contentType,
                                   UserId userId, TenantId tenantId) {
        private GenerateContext {
        }

        private GenerateContext(SessionId sessionId, WorkspaceContext context, ContentType contentType, UserIdentity identity) {
            this(sessionId, context, contentType, identity.userId(), identity.tenantId());
        }
    }
}
