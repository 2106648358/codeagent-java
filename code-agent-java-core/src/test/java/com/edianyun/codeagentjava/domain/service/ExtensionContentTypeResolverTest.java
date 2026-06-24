package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.service.impl.ExtensionContentTypeResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExtensionContentTypeResolver 单元测试。
 * 验证基于文件扩展名的内容类型推断逻辑。
 */
class ExtensionContentTypeResolverTest {

    private final ExtensionContentTypeResolver resolver = new ExtensionContentTypeResolver();

    @ParameterizedTest
    @CsvSource({
            "App.java, CODE",
            "README.md, MARKDOWN",
            "config.json, JSON",
            "docker-compose.yml, YAML",
            "notes.txt, TEXT",
    })
    void shouldResolveTypeByExtension(String fileName, ContentType expected) {
        assertThat(resolver.resolve(fileName)).isEqualTo(expected);
    }

    @Test
    void shouldReturnUnknownForNullFileName() {
        assertThat(resolver.resolve(null)).isEqualTo(ContentType.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownForBlanFileName() {
        assertThat(resolver.resolve("")).isEqualTo(ContentType.UNKNOWN);
        assertThat(resolver.resolve("  ")).isEqualTo(ContentType.UNKNOWN);
    }

    @Test
    void shouldReturnTextForFileWithoutExtension() {
        assertThat(resolver.resolve("Makefile")).isEqualTo(ContentType.TEXT);
    }

    @Test
    void shouldReturnTextForFileWithTrailingDot() {
        assertThat(resolver.resolve("config.")).isEqualTo(ContentType.TEXT);
    }

    @Test
    void shouldHandleFullPathInput() {
        assertThat(resolver.resolve("/home/user/project/src/Main.java")).isEqualTo(ContentType.CODE);
    }
}
