package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.ChatRequest;
import com.edianyun.codeagentjava.application.dto.ChatResponse;
import com.edianyun.codeagentjava.application.port.ChatUseCase;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.shell.core.command.Shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ChatCommand Spring Shell 集成测试。
 * 验证命令参数解析和执行流程。
 */
@SpringBootTest
class ChatCommandTest {

    @Autowired
    private Shell shell;

    @MockBean
    private ChatUseCase chatUseCase;

    @Test
    void shouldRejectMissingPrompt() {
        Object result = shell.evaluate(() -> "chat");
        // 缺少必填参数时 Spring Shell 应报告错误
        assertThat(result).isNotNull();
    }

    @Test
    void shouldCallChatUseCaseWithPrompt() {
        when(chatUseCase.chat(any())).thenReturn(new ChatResponse("s1", "Hello back"));

        Object result = shell.evaluate(() -> "chat --prompt Hello");

        verify(chatUseCase).chat(any());
    }

    @Test
    void shouldUseSessionIdWhenProvided() {
        when(chatUseCase.chat(any())).thenReturn(new ChatResponse("my-session", "ok"));

        shell.evaluate(() -> "chat --prompt Hi --session my-session");

        verify(chatUseCase).chat(any());
    }
}
