package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.ExplainResponse;
import com.edianyun.codeagentjava.application.port.ExplainUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.shell.core.command.Shell;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExplainCommand Spring Shell 集成测试。
 * 验证解释命令的参数解析和执行流程。
 */
@SpringBootTest
class ExplainCommandTest {

    @Autowired
    private Shell shell;

    @MockBean
    private ExplainUseCase explainUseCase;

    @Test
    void shouldCallExplainUseCaseWithTarget() {
        when(explainUseCase.explain(any())).thenReturn(
                new ExplainResponse("pom.xml", "This is the build config"));

        shell.evaluate(() -> "explain --target pom.xml");

        verify(explainUseCase).explain(any());
    }

    @Test
    void shouldAcceptScopeParameter() {
        when(explainUseCase.explain(any())).thenReturn(
                new ExplainResponse("App.java", "A main class"));

        shell.evaluate(() -> "explain --target App.java --scope core");

        verify(explainUseCase).explain(any());
    }

    @Test
    void shouldAcceptSessionParameter() {
        when(explainUseCase.explain(any())).thenReturn(
                new ExplainResponse("test", "ok"));

        shell.evaluate(() -> "explain --target test --session explain-session");

        verify(explainUseCase).explain(any());
    }
}
