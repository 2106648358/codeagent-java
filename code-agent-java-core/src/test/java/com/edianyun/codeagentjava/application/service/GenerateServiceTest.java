package com.edianyun.codeagentjava.application.service;

import com.edianyun.codeagentjava.application.dto.GenerateRequest;
import com.edianyun.codeagentjava.application.dto.GenerateResponse;
import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;
import com.edianyun.codeagentjava.domain.service.ContentFragmentExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GenerateService 应用服务测试（使用 Mock 端口）。
 * 验证生成流程编排：工作区扫描 → LLM 生成 → 内容提取 → 持久化 → 遥测记录。
 */
@ExtendWith(MockitoExtension.class)
class GenerateServiceTest {

    @Mock
    private AgentOrchestrator agentOrchestrator;
    @Mock
    private StreamingAgentOrchestrator streamingAgentOrchestrator;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private LocalStorage localStorage;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @Mock
    private TelemetryCollector telemetryCollector;
    @Mock
    private ContentFragmentExtractor contentFragmentExtractor;

    private GenerateService generateService;

    @BeforeEach
    void setUp() {
        generateService = new GenerateService(agentOrchestrator, streamingAgentOrchestrator,
                fileRepository, localStorage, userIdentityResolver,
                telemetryCollector, contentFragmentExtractor);
    }

    @Test
    void shouldGenerateContentAndReturnFragments() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(fileRepository.scan(any(), any())).thenReturn(
                new WorkspaceContext(java.nio.file.Path.of("."), List.of(), List.of(), ContentType.CODE, null));
        when(agentOrchestrator.generate(any(), any())).thenAnswer(invocation -> {
            GenerationTask task = invocation.getArgument(0);
            task.addFragment(ContentFragment.of(new RelativePath("App.java"), "class App {}", ContentType.CODE));
            task.complete();
            return task;
        });

        GenerateRequest request = new GenerateRequest(null, null, "Create a class", ContentType.CODE, List.of());
        GenerateResponse response = generateService.generate(request);

        assertThat(response.fragments()).hasSize(1);
        assertThat(response.fragments().get(0).path().value()).isEqualTo("App.java");
        verify(localStorage).saveGenerationTask(any());
        verify(telemetryCollector).record(any());
    }

    @Test
    void shouldScanWorkspaceBeforeGeneration() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(fileRepository.scan(any(), any())).thenReturn(
                new WorkspaceContext(java.nio.file.Path.of("."), List.of(), List.of(), ContentType.CODE, null));
        when(agentOrchestrator.generate(any(), any())).thenAnswer(invocation -> {
            GenerationTask task = invocation.getArgument(0);
            task.complete();
            return task;
        });

        generateService.generate(new GenerateRequest(null, null, "test", ContentType.CODE, List.of()));

        verify(fileRepository).scan(any(), any());
    }

    @Test
    void shouldReturnGeneratedTaskId() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(fileRepository.scan(any(), any())).thenReturn(
                new WorkspaceContext(java.nio.file.Path.of("."), List.of(), List.of(), ContentType.CODE, null));
        when(agentOrchestrator.generate(any(), any())).thenAnswer(invocation -> {
            GenerationTask task = invocation.getArgument(0);
            task.complete();
            return task;
        });

        GenerateRequest request = new GenerateRequest(null, null, "test", ContentType.CODE, List.of());
        GenerateResponse response = generateService.generate(request);

        assertThat(response.taskId()).isNotNull();
        assertThat(response.sessionId()).isNotNull();
    }
}
