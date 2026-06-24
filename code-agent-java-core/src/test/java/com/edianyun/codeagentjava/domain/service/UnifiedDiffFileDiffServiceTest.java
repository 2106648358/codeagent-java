package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.service.impl.UnifiedDiffFileDiffService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UnifiedDiffFileDiffService 单元测试。
 * 验证行级别 diff 计算的正确性，包括新增、删除、修改和无变化场景。
 */
class UnifiedDiffFileDiffServiceTest {

    private final UnifiedDiffFileDiffService service = new UnifiedDiffFileDiffService();

    @Test
    void shouldReturnNoChangesForIdenticalContent() {
        String result = service.diff("hello\nworld", "hello\nworld");
        assertThat(result).isEqualTo("No changes.");
    }

    @Test
    void shouldDetectNoChangesForBothNull() {
        String result = service.diff(null, null);
        assertThat(result).isEqualTo("No changes.");
    }

    @Test
    void shouldDetectNoChangesForBothEmpty() {
        String result = service.diff("", "");
        assertThat(result).isEqualTo("No changes.");
    }

    @Test
    void shouldDetectAddedLine() {
        String result = service.diff("a\nb", "a\nb\nc");
        assertThat(result).contains("+c");
    }

    @Test
    void shouldDetectRemovedLine() {
        String result = service.diff("a\nb\nc", "a\nc");
        assertThat(result).contains("-b");
    }

    @Test
    void shouldDetectModifiedLine() {
        String result = service.diff("hello world", "hello Java");
        assertThat(result).contains("-hello world");
        assertThat(result).contains("+hello Java");
    }

    @Test
    void shouldHandleNullOldContent() {
        String result = service.diff(null, "new content");
        assertThat(result).contains("+new content");
    }

    @Test
    void shouldHandleNullNewContent() {
        String result = service.diff("old content", null);
        assertThat(result).contains("-old content");
    }

    @Test
    void shouldIncludeDiffHeader() {
        String result = service.diff("a", "b");
        assertThat(result).contains("--- old");
        assertThat(result).contains("+++ new");
    }
}
