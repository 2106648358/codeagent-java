package com.edianyun.codeagentjava.cli.converter;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ContentTypeConverter 单元测试。
 * 验证字符串到 ContentType 的转换逻辑，包括枚举名直接匹配和扩展名推断。
 */
class ContentTypeConverterTest {

    private final ContentTypeConverter converter = new ContentTypeConverter();

    @Test
    void shouldConvertEnumName() {
        assertThat(converter.convert("CODE")).isEqualTo(ContentType.CODE);
        assertThat(converter.convert("markdown")).isEqualTo(ContentType.MARKDOWN);
        assertThat(converter.convert("JSON")).isEqualTo(ContentType.JSON);
    }

    @Test
    void shouldConvertExtensionToType() {
        assertThat(converter.convert("java")).isEqualTo(ContentType.CODE);
        assertThat(converter.convert("md")).isEqualTo(ContentType.MARKDOWN);
        assertThat(converter.convert("yml")).isEqualTo(ContentType.YAML);
    }

    @Test
    void shouldReturnUnknownForNull() {
        assertThat(converter.convert(null)).isEqualTo(ContentType.UNKNOWN);
    }

    @Test
    void shouldReturnUnknownForBlan() {
        assertThat(converter.convert("")).isEqualTo(ContentType.UNKNOWN);
        assertThat(converter.convert("  ")).isEqualTo(ContentType.UNKNOWN);
    }
}
