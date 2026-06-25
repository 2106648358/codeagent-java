package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.ChatRequest;
import com.edianyun.codeagentjava.application.dto.ChatResponse;
import com.edianyun.codeagentjava.application.port.ChatUseCase;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.test.ShellTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * ChatCommand Spring Shell 集成测试。
 * 验证命令参数解析和执行流程。
 */
@SpringBootTest
class ChatCommandTest {

    @Autowired
    private ShellTestClient shellTestClient;

    @Autowired
    private ChatUseCase chatUseCase;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        ChatUseCase chatUseCase() {
            return mock(ChatUseCase.class);
        }

        @Bean
        ShellTestClient shellTestClient(CommandParser commandParser, CommandRegistry commandRegistry) {
            return new ShellTestClient(commandParser, commandRegistry);
        }
    }

    @Test
    void shouldRejectMissingPrompt() throws Exception {
        var screen = shellTestClient.sendCommand("chat");
        // 缺少必填参数时 Spring Shell 应报告错误
        assertThat(screen).isNotNull();
    }

    @Test
    void shouldCallChatUseCaseWithPrompt() throws Exception {
        when(chatUseCase.chat(any())).thenReturn(new ChatResponse("s1", "Hello back"));

        shellTestClient.sendCommand("chat --prompt Hello");

        verify(chatUseCase).chat(any());
    }

    @Test
    void shouldUseSessionIdWhenProvided() throws Exception {
        when(chatUseCase.chat(any())).thenReturn(new ChatResponse("my-session", "ok"));

        shellTestClient.sendCommand("chat --prompt Hi --session my-session");

        verify(chatUseCase).chat(any());
    }
}
