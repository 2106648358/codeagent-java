package com.edianyun.codeagentjava.application.service;

import com.edianyun.codeagentjava.application.dto.ChatRequest;
import com.edianyun.codeagentjava.application.dto.ChatResponse;
import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatService 应用服务测试（使用 Mock 端口）。
 * 验证多轮对话、新会话创建、会话持久化和遥测记录等编排逻辑。
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AgentOrchestrator agentOrchestrator;
    @Mock
    private StreamingAgentOrchestrator streamingAgentOrchestrator;
    @Mock
    private LocalStorage localStorage;
    @Mock
    private UserIdentityResolver userIdentityResolver;
    @Mock
    private TelemetryCollector telemetryCollector;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(agentOrchestrator, streamingAgentOrchestrator,
                localStorage, userIdentityResolver, telemetryCollector);
    }

    @Test
    void shouldCreateNewSessionWhenNoSessionIdProvided() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(localStorage.loadSession(any())).thenReturn(Optional.empty());
        when(agentOrchestrator.chat(any(), any()))
                .thenReturn(Message.assistant("Hi!", null, null));

        ChatRequest request = new ChatRequest(null, null, "Hello");
        ChatResponse response = chatService.chat(request);

        assertThat(response.sessionId()).isNotNull();
        assertThat(response.reply()).isEqualTo("Hi!");
        verify(localStorage).saveSession(any());
        verify(telemetryCollector).record(any());
    }

    @Test
    void shouldLoadExistingSessionWhenSessionIdProvided() {
        SessionId existingId = SessionId.generate();
        Session existingSession = new Session(existingId, UserId.anonymous(),
                TenantId.defaultTenant(), "Test Session");

        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(localStorage.loadSession(existingId)).thenReturn(Optional.of(existingSession));
        when(agentOrchestrator.chat(any(), any()))
                .thenReturn(Message.assistant("Response", null, null));

        ChatRequest request = new ChatRequest(existingId.value(), null, "Continue");
        ChatResponse response = chatService.chat(request);

        assertThat(response.sessionId()).isEqualTo(existingId.value());
        assertThat(response.reply()).isEqualTo("Response");
        verify(localStorage).saveSession(any());
    }

    @Test
    void shouldResolveUserIdentityOnEachCall() {
        UserIdentity identity = new UserIdentity(new UserId("dev-1"), TenantId.defaultTenant());
        when(userIdentityResolver.resolve()).thenReturn(identity);
        when(localStorage.loadSession(any())).thenReturn(Optional.empty());
        when(agentOrchestrator.chat(any(), any()))
                .thenReturn(Message.assistant("Hi", null, null));

        chatService.chat(new ChatRequest(null, null, "Hello"));

        verify(userIdentityResolver).resolve();
    }

    @Test
    void shouldRecordTelemetryOnSuccess() {
        when(userIdentityResolver.resolve()).thenReturn(new UserIdentity(
                UserId.anonymous(), TenantId.defaultTenant()));
        when(localStorage.loadSession(any())).thenReturn(Optional.empty());
        when(agentOrchestrator.chat(any(), any()))
                .thenReturn(Message.assistant("Done", null, null));

        chatService.chat(new ChatRequest(null, null, "Hello"));

        verify(telemetryCollector).record(any());
    }
}
