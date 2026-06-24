package com.edianyun.codeagentjava.application.service;

import com.edianyun.codeagentjava.application.dto.ExplainRequest;
import com.edianyun.codeagentjava.application.dto.ExplainResponse;
import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;
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
 * ExplainService 应用服务测试（使用 Mock 端口）。
 * 验证解释流程编排：工作区扫描 → LLM 解释 → 遥测记录。
 */
@ExtendWith(MockitoExtension.class)
class ExplainServiceTest {

    @Mock
    private AgentOrchestrator agentOrchestrator;
    @Mock
    private StreamingAgentOrchestrator streamingAgentOrchestrator;
    @Mock
    private FileRepository fileRepository;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @Mock
    private TelemetryCollector telemetryCollector;

    private ExplainService explainService;

    @BeforeEach
    void setUp() {
        explainService = new ExplainService(agentOrchestrator, streamingAgentOrchestrator,
                fileRepository, userIdentityResolver, telemetryCollector);
    }

    @Test
    void shouldExplainTargetAndReturnResult() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(fileRepository.scan(any(), any())).thenReturn(
                new WorkspaceContext(java.nio.file.Path.of("."), List.of(), List.of(),
                        com.edianyun.codeagentjava.domain.model.content.ContentType.CODE, null));
        when(agentOrchestrator.explain(any(), any()))
                .thenReturn("This file configures the project build.");

        ExplainRequest request = new ExplainRequest(null, null, "pom.xml", "");
        ExplainResponse response = explainService.explain(request);

        assertThat(response.target()).isEqualTo("pom.xml");
        assertThat(response.explanation()).isEqualTo("This file configures the project build.");
        verify(telemetryCollector).record(any());
    }

    @Test
    void shouldScanWorkspaceForContext() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(fileRepository.scan(any(), any())).thenReturn(
                new WorkspaceContext(java.nio.file.Path.of("."), List.of(), List.of(),
                        com.edianyun.codeagentjava.domain.model.content.ContentType.CODE, null));
        when(agentOrchestrator.explain(any(), any())).thenReturn("explanation");

        explainService.explain(new ExplainRequest(null, null, "test", ""));

        verify(fileRepository).scan(any(), any());
    }

    @Test
    void shouldRecordTelemetryOnSuccess() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(fileRepository.scan(any(), any())).thenReturn(
                new WorkspaceContext(java.nio.file.Path.of("."), List.of(), List.of(),
                        com.edianyun.codeagentjava.domain.model.content.ContentType.CODE, null));
        when(agentOrchestrator.explain(any(), any())).thenReturn("done");

        explainService.explain(new ExplainRequest(null, null, "target", ""));

        verify(telemetryCollector).record(any());
    }
}
