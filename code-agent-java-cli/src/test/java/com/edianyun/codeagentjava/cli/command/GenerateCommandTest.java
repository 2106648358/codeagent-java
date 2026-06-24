package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.GenerateResponse;
import com.edianyun.codeagentjava.application.port.GenerateUseCase;
import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.shell.core.command.Shell;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GenerateCommand Spring Shell 集成测试。
 * 验证生成命令的参数解析和执行流程。
 */
@SpringBootTest
class GenerateCommandTest {

    @Autowired
    private Shell shell;

    @MockBean
    private GenerateUseCase generateUseCase;

    @MockBean
    private FileRepository fileRepository;

    @Test
    void shouldCallGenerateUseCaseWithRequirements() {
        when(generateUseCase.generate(any())).thenReturn(
                new GenerateResponse("task-1", "s1", List.of(
                        ContentFragment.of(new RelativePath("App.java"), "code", ContentType.CODE)
                )));

        shell.evaluate(() -> "generate --requirements 'Create a class' --yes");

        verify(generateUseCase).generate(any());
    }

    @Test
    void shouldSkipConfirmationWithYesFlag() {
        when(generateUseCase.generate(any())).thenReturn(
                new GenerateResponse("task-2", "s1", List.of(
                        ContentFragment.of(new RelativePath("test.txt"), "text", ContentType.TEXT)
                )));

        Object result = shell.evaluate(() -> "generate --requirements 'test' --yes");

        verify(generateUseCase).generate(any());
    }

    @Test
    void shouldAcceptContentTypeParameter() {
        when(generateUseCase.generate(any())).thenReturn(
                new GenerateResponse("task-3", "s1", List.of()));

        shell.evaluate(() -> "generate --requirements 'test' --type JSON --yes");

        verify(generateUseCase).generate(any());
    }
}
