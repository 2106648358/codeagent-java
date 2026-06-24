package com.edianyun.codeagentjava.integration;

import com.edianyun.codeagentjava.application.port.ChatUseCase;
import com.edianyun.codeagentjava.application.port.ExplainUseCase;
import com.edianyun.codeagentjava.application.port.GenerateUseCase;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.shell.core.command.Shell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 端到端 CLI 集成测试。
 * 启动完整 Spring Shell 上下文，使用 MockBean 替换所有外部端口，
 * 验证 chat / generate / explain 三条命令的完整执行流程。
 * <p>
 * 测试策略：
 * - 使用 @MockBean 替换所有 UseCase 和外部依赖
 * - 不需要真实 LLM / 数据库 / 文件系统
 * - Shell.evaluate() 模拟用户在终端输入命令
 */
@SpringBootTest
class CliIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Shell shell;

    @MockBean
    private ChatUseCase chatUseCase;

    @MockBean
    private GenerateUseCase generateUseCase;

    @MockBean
    private ExplainUseCase explainUseCase;

    // ---- 上下文启动验证 ----

    @Test
    void shouldLoadApplicationContext() {
        assertThat(context).isNotNull();
        assertThat(context.containsBean("chatCommand")).isTrue();
        assertThat(context.containsBean("generateCommand")).isTrue();
        assertThat(context.containsBean("explainCommand")).isTrue();
    }

    // ---- Chat 流程 ----

    @Test
    void shouldExecuteChatCommandSuccessfully() {
        when(chatUseCase.chat(any())).thenReturn(
                new com.edianyun.codeagentjava.application.dto.ChatResponse("session-1", "Hello from AI"));

        Object result = shell.evaluate(() -> "chat --prompt 'What is Java?'");

        assertThat(result).isNotNull();
    }

    // ---- Generate 流程 ----

    @Test
    void shouldExecuteGenerateCommandSuccessfully() {
        when(generateUseCase.generate(any())).thenReturn(
                new com.edianyun.codeagentjava.application.dto.GenerateResponse("task-1", "s1",
                        java.util.List.of(
                                com.edianyun.codeagentjava.domain.model.content.ContentFragment.of(
                                        new RelativePath("Hello.java"),
                                        "public class Hello {}",
                                        ContentType.CODE))));

        Object result = shell.evaluate(() -> "generate --requirements 'Create a HelloWorld' --yes");

        assertThat(result).isNotNull();
    }

    // ---- Explain 流程 ----

    @Test
    void shouldExecuteExplainCommandSuccessfully() {
        when(explainUseCase.explain(any())).thenReturn(
                new com.edianyun.codeagentjava.application.dto.ExplainResponse("pom.xml",
                        "This file defines the Maven build configuration."));

        Object result = shell.evaluate(() -> "explain --target pom.xml");

        assertThat(result).isNotNull();
    }

    // ---- 组合流程 ----

    @Test
    void shouldNavigateChatToGenerateToExplain() {
        when(chatUseCase.chat(any())).thenReturn(
                new com.edianyun.codeagentjava.application.dto.ChatResponse("s-multi", "Sure, I can help"));
        when(generateUseCase.generate(any())).thenReturn(
                new com.edianyun.codeagentjava.application.dto.GenerateResponse("t-multi", "s-multi", java.util.List.of()));
        when(explainUseCase.explain(any())).thenReturn(
                new com.edianyun.codeagentjava.application.dto.ExplainResponse("file.txt", "A text file"));

        // 同一会话中依次执行三条命令
        shell.evaluate(() -> "chat --prompt 'Hello' --session multi");
        shell.evaluate(() -> "generate --requirements 'test' --session multi --yes");
        shell.evaluate(() -> "explain --target file.txt --session multi");

        // 验证所有命令均可正常执行（不抛异常）
    }
}
