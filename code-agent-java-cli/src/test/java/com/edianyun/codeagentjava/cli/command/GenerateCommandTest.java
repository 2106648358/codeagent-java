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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.test.ShellTestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * GenerateCommand Spring Shell 集成测试。
 * 验证生成命令的参数解析和执行流程。
 */
@SpringBootTest
class GenerateCommandTest {

    @Autowired
    private ShellTestClient shellTestClient;

    @Autowired
    private GenerateUseCase generateUseCase;

    @Autowired
    private FileRepository fileRepository;

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        GenerateUseCase generateUseCase() {
            return mock(GenerateUseCase.class);
        }

        @Bean
        @Primary
        FileRepository fileRepository() {
            return mock(FileRepository.class);
        }

        @Bean
        ShellTestClient shellTestClient(CommandParser commandParser, CommandRegistry commandRegistry) {
            return new ShellTestClient(commandParser, commandRegistry);
        }
    }

    @Test
    void shouldCallGenerateUseCaseWithRequirements() throws Exception {
        when(generateUseCase.generate(any())).thenReturn(
                new GenerateResponse("task-1", "s1", List.of(
                        ContentFragment.of(new RelativePath("App.java"), "code", ContentType.CODE)
                )));

        shellTestClient.sendCommand("generate --requirements 'Create a class' --yes");

        verify(generateUseCase).generate(any());
    }

    @Test
    void shouldSkipConfirmationWithYesFlag() throws Exception {
        when(generateUseCase.generate(any())).thenReturn(
                new GenerateResponse("task-2", "s1", List.of(
                        ContentFragment.of(new RelativePath("test.txt"), "text", ContentType.TEXT)
                )));

        shellTestClient.sendCommand("generate --requirements 'test' --yes");

        verify(generateUseCase).generate(any());
    }

    @Test
    void shouldAcceptContentTypeParameter() throws Exception {
        when(generateUseCase.generate(any())).thenReturn(
                new GenerateResponse("task-3", "s1", List.of()));

        shellTestClient.sendCommand("generate --requirements 'test' --type JSON --yes");

        verify(generateUseCase).generate(any());
    }
}
