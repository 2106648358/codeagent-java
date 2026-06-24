package com.edianyun.codeagentjava.domain.model.content;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ContentType 枚举单元测试。
 * 验证内容类型推断逻辑。
 */
class ContentTypeTest {

    @ParameterizedTest
    @CsvSource({
            "md, MARKDOWN",
            "markdown, MARKDOWN",
            "json, JSON",
            "yml, YAML",
            "yaml, YAML",
            "txt, TEXT",
            "text, TEXT",
            "java, CODE",
            "py, CODE",
            "xml, CODE"
    })
    void shouldInferTypeFromExtension(String extension, ContentType expected) {
        assertThat(ContentType.fromExtension(extension)).isEqualTo(expected);
    }

    @Test
    void shouldReturnUnknownForNullExtension() {
        assertThat(ContentType.fromExtension(null)).isEqualTo(ContentType.UNKNOWN);
    }

    @Test
    void shouldReturnCodeForUnrecognizedExtension() {
        assertThat(ContentType.fromExtension("bin")).isEqualTo(ContentType.CODE);
    }
}
