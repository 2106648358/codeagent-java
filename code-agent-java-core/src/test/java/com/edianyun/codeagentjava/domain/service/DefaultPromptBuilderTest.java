package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Prompt;
import com.edianyun.codeagentjava.domain.model.session.SessionId;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.service.impl.DefaultPromptBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultPromptBuilder 单元测试。
 * 验证三种 Prompt 构建策略的正确性。
 */
class DefaultPromptBuilderTest {

    private final DefaultPromptBuilder builder = new DefaultPromptBuilder();

    @Test
    void shouldBuildGenerationPromptWithRequirements() {
        GenerationTask task = new GenerationTask(UUID.randomUUID(), SessionId.generate(),
                "Create a REST controller", ContentType.CODE);
        WorkspaceContext ctx = new WorkspaceContext(Path.of("/tmp"),
                List.of(new RelativePath("src/Main.java")),
                List.of(), ContentType.CODE, null);

        Prompt prompt = builder.buildGenerationPrompt(task, ctx);

        assertThat(prompt.role()).isEqualTo("system");
        assertThat(prompt.content()).contains("Create a REST controller");
        assertThat(prompt.content()).contains("Target content type: CODE");
        assertThat(prompt.content()).contains("src/Main.java");
    }

    @Test
    void shouldBuildGenerationPromptWithSelectedFiles() {
        GenerationTask task = new GenerationTask(UUID.randomUUID(), SessionId.generate(),
                "refactor", ContentType.CODE);
        WorkspaceContext ctx = new WorkspaceContext(Path.of("/tmp"),
                List.of(new RelativePath("src/Main.java")),
                List.of(new RelativePath("src/App.java")),
                ContentType.CODE, null);

        Prompt prompt = builder.buildGenerationPrompt(task, ctx);

        assertThat(prompt.content()).contains("Selected files for context");
        assertThat(prompt.content()).contains("src/App.java");
    }

    @Test
    void shouldBuildExplanationPrompt() {
        WorkspaceContext ctx = new WorkspaceContext(Path.of("/tmp"),
                List.of(new RelativePath("pom.xml")),
                List.of(), ContentType.CODE, null);

        Prompt prompt = builder.buildExplanationPrompt("pom.xml", ctx);

        assertThat(prompt.role()).isEqualTo("system");
        assertThat(prompt.content()).contains("Target: pom.xml");
        assertThat(prompt.content()).contains("pom.xml");
    }

    @Test
    void shouldBuildChatPrompt() {
        WorkspaceContext ctx = new WorkspaceContext(Path.of("/tmp"),
                List.of(new RelativePath("README.md")),
                List.of(), ContentType.MARKDOWN, null);

        Prompt prompt = builder.buildChatPrompt("What is this project?", ctx);

        assertThat(prompt.role()).isEqualTo("user");
        assertThat(prompt.content()).contains("What is this project?");
        assertThat(prompt.content()).contains("README.md");
    }

    @Test
    void shouldBuildChatPromptWithoutFiles() {
        WorkspaceContext ctx = new WorkspaceContext(Path.of("/tmp"),
                List.of(), List.of(), ContentType.TEXT, null);

        Prompt prompt = builder.buildChatPrompt("Hello", ctx);

        assertThat(prompt.content()).contains("Hello");
        assertThat(prompt.content()).doesNotContain("Current workspace files");
    }
}
