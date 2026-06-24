package com.edianyun.codeagentjava.cli.converter;

import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RelativePathConverter 单元测试。
 * 验证字符串到 RelativePath 的转换逻辑。
 */
class RelativePathConverterTest {

    private final RelativePathConverter converter = new RelativePathConverter();

    @Test
    void shouldConvertValidRelativePath() {
        RelativePath path = converter.convert("src/main/App.java");
        assertThat(path.value()).isEqualTo("src/main/App.java");
    }

    @Test
    void shouldRejectAbsolutePath() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("/etc/hosts"));
    }

    @Test
    void shouldRejectBlan() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert(""));
    }
}
