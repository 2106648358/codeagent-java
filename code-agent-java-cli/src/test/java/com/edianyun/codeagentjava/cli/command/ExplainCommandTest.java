package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.ExplainResponse;
import com.edianyun.codeagentjava.application.port.ExplainUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.test.ShellTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * ExplainCommand Spring Shell 集成测试。
 * 验证解释命令的参数解析和执行流程。
 */
@SpringBootTest
class ExplainCommandTest {

    @Autowired
    private ShellTestClient shellTestClient;

    @Autowired
    private ExplainUseCase explainUseCase;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        ExplainUseCase explainUseCase() {
            return mock(ExplainUseCase.class);
        }

        @Bean
        ShellTestClient shellTestClient(CommandParser commandParser, CommandRegistry commandRegistry) {
            return new ShellTestClient(commandParser, commandRegistry);
        }
    }

    @Test
    void shouldCallExplainUseCaseWithTarget() throws Exception {
        when(explainUseCase.explain(any())).thenReturn(
                new ExplainResponse("pom.xml", "This is the build config"));

        shellTestClient.sendCommand("explain --target pom.xml");

        verify(explainUseCase).explain(any());
    }

    @Test
    void shouldAcceptScopeParameter() throws Exception {
        when(explainUseCase.explain(any())).thenReturn(
                new ExplainResponse("App.java", "A main class"));

        shellTestClient.sendCommand("explain --target App.java --scope core");

        verify(explainUseCase).explain(any());
    }

    @Test
    void shouldAcceptSessionParameter() throws Exception {
        when(explainUseCase.explain(any())).thenReturn(
                new ExplainResponse("test", "ok"));

        shellTestClient.sendCommand("explain --target test --session explain-session");

        verify(explainUseCase).explain(any());
    }
}
