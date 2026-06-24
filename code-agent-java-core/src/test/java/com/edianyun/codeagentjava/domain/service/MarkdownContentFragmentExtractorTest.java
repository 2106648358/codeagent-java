package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.service.impl.MarkdownContentFragmentExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarkdownContentFragmentExtractor 单元测试。
 * 验证从 Markdown 代码块中提取 ContentFragment 的正确性。
 */
class MarkdownContentFragmentExtractorTest {

    private final MarkdownContentFragmentExtractor extractor = new MarkdownContentFragmentExtractor();

    @Test
    void shouldExtractSingleCodeBlock() {
        String input = "```java src/main/App.java\npublic class App {}\n```";
        List<ContentFragment> fragments = extractor.extract(input, ContentType.CODE);

        assertThat(fragments).hasSize(1);
        assertThat(fragments.get(0).path().value()).isEqualTo("src/main/App.java");
        assertThat(fragments.get(0).content()).contains("public class App {}");
    }

    @Test
    void shouldExtractMultipleCodeBlocks() {
        String input = "```java src/App.java\nclass App {}\n```\n\n```json src/config.json\n{\"key\":\"value\"}\n```";
        List<ContentFragment> fragments = extractor.extract(input, ContentType.CODE);

        assertThat(fragments).hasSize(2);
        assertThat(fragments.get(0).path().value()).isEqualTo("src/App.java");
        assertThat(fragments.get(1).path().value()).isEqualTo("src/config.json");
    }

    @Test
    void shouldReturnFallbackForNoCodeBlock() {
        String input = "Just plain text without code blocks";
        List<ContentFragment> fragments = extractor.extract(input, ContentType.TEXT);

        assertThat(fragments).hasSize(1);
        assertThat(fragments.get(0).path().value()).isEqualTo("generated.txt");
        assertThat(fragments.get(0).content()).isEqualTo(input);
    }

    @Test
    void shouldReturnEmptyListForNullInput() {
        List<ContentFragment> fragments = extractor.extract(null, ContentType.CODE);
        assertThat(fragments).isEmpty();
    }

    @Test
    void shouldReturnEmptyListForBlanInput() {
        List<ContentFragment> fragments = extractor.extract("   ", ContentType.CODE);
        assertThat(fragments).isEmpty();
    }

    @Test
    void shouldResolveContentTypeFromCodeBlockInfo() {
        String input = "```json data.json\n{\"a\":1}\n```";
        List<ContentFragment> fragments = extractor.extract(input, ContentType.CODE);

        assertThat(fragments).hasSize(1);
        assertThat(fragments.get(0).contentType()).isEqualTo(ContentType.JSON);
    }

    @Test
    void shouldFallbackToDefaultTypeWhenUnknown() {
        String input = "```unknown\nsome content\n```";
        List<ContentFragment> fragments = extractor.extract(input, ContentType.CODE);

        assertThat(fragments).hasSize(1);
        assertThat(fragments.get(0).contentType()).isEqualTo(ContentType.CODE);
    }

    @Test
    void shouldExtractPathFromCommentHint() {
        String input = "```java\n// path: src/main/App.java\npublic class App {}\n```";
        List<ContentFragment> fragments = extractor.extract(input, ContentType.CODE);

        assertThat(fragments).hasSize(1);
        assertThat(fragments.get(0).path().value()).isEqualTo("src/main/App.java");
    }
}
