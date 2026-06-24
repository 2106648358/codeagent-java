package com.edianyun.codeagentjava.domain.model.workspace;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RelativePath 值对象单元测试。
 * 验证相对路径校验（拒绝对路径）和路径解析。
 */
class RelativePathTest {

    @Test
    void shouldCreateValidRelativePath() {
        RelativePath rp = new RelativePath("src/main/App.java");
        assertThat(rp.value()).isEqualTo("src/main/App.java");
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new RelativePath(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new RelativePath(""));
        assertThrows(IllegalArgumentException.class, () -> new RelativePath("  "));
    }

    @Test
    void shouldRejectAbsoluteUnixPath() {
        assertThrows(IllegalArgumentException.class, () -> new RelativePath("/etc/hosts"));
    }

    @Test
    void shouldRejectAbsoluteWindowsPath() {
        assertThrows(IllegalArgumentException.class, () -> new RelativePath("\\Windows\\System32"));
    }

    @Test
    void shouldAcceptPathWithBackslashesInMiddle() {
        RelativePath rp = new RelativePath("src\\main\\App.java");
        assertThat(rp.value()).isEqualTo("src\\main\\App.java");
    }

    @Test
    void shouldResolveToAbsolutePath() {
        Path base = Path.of("/home/user/project");
        RelativePath rp = new RelativePath("src/App.java");
        assertThat(rp.toPath(base).toString())
                .endsWith("src/App.java");
    }

    @Test
    void shouldSupportEquality() {
        assertThat(new RelativePath("a/b")).isEqualTo(new RelativePath("a/b"));
        assertThat(new RelativePath("a/b")).isNotEqualTo(new RelativePath("c/d"));
    }
}
