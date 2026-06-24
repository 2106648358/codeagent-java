package com.edianyun.codeagentjava.cli.output;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.FileChange;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.service.impl.UnifiedDiffFileDiffService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TerminalRenderer 单元测试。
 * 验证 diff 渲染输出内容正确性（不测试交互式 confirm）。
 */
class TerminalRendererTest {

    private TerminalRenderer renderer;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        renderer = new TerminalRenderer(new UnifiedDiffFileDiffService());
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void shouldRenderDiffWithHeader() {
        FileChange change = FileChange.update(new RelativePath("test.txt"), "old", "new");
        renderer.renderDiff(change);

        String output = outputStream.toString();
        assertThat(output).contains("=== test.txt ===");
        assertThat(output).contains("--- old");
        assertThat(output).contains("+++ new");
    }

    @Test
    void shouldRenderFragmentsWithSeparator() {
        var fragments = java.util.List.of(
                com.edianyun.codeagentjava.domain.model.content.ContentFragment.of(
                        new RelativePath("file1.txt"), "content1", ContentType.TEXT),
                com.edianyun.codeagentjava.domain.model.content.ContentFragment.of(
                        new RelativePath("file2.txt"), "content2", ContentType.TEXT)
        );
        renderer.renderFragments(fragments);

        String output = outputStream.toString();
        assertThat(output).contains("--- file1.txt ---");
        assertThat(output).contains("content1");
        assertThat(output).contains("--- file2.txt ---");
        assertThat(output).contains("content2");
    }

    @Test
    void shouldPrintTokenWithoutNewline() {
        renderer.printToken("Hello");
        renderer.printToken(" ");
        renderer.printToken("World");
        renderer.flush();

        String output = outputStream.toString();
        assertThat(output).startsWith("Hello World");
    }
}
