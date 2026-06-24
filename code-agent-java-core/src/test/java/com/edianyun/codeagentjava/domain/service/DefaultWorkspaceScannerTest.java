package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.service.impl.DefaultWorkspaceScanner;
import com.edianyun.codeagentjava.domain.service.impl.ExtensionContentTypeResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultWorkspaceScanner 单元测试。
 * 验证工作区扫描的基本功能（扫描结果包含文件列表、主要内容类型推断等）。
 */
class DefaultWorkspaceScannerTest {

    private final DefaultWorkspaceScanner scanner = new DefaultWorkspaceScanner(new ExtensionContentTypeResolver());

    @Test
    void shouldScanCurrentDirectory() {
        var ctx = scanner.scan(java.nio.file.Path.of("."), null);

        assertThat(ctx).isNotNull();
        assertThat(ctx.workingDir()).isNotNull();
        assertThat(ctx.files()).isNotEmpty();
    }

    @Test
    void shouldUseDefaultOptionsWhenNull() {
        var ctx = scanner.scan(java.nio.file.Path.of("."), null);

        assertThat(ctx).isNotNull();
        assertThat(ctx.files()).isNotEmpty();
    }

    @Test
    void shouldInferPrimaryContentType() {
        var ctx = scanner.scan(java.nio.file.Path.of("."), null);

        assertThat(ctx.primaryType()).isNotNull();
        // 当前目录包含 .java 文件，主要类型应为 CODE
    }

    @Test
    void shouldReturnEmptyForEmptyDirectory() throws Exception {
        java.nio.file.Path tmpDir = java.nio.file.Files.createTempDirectory("empty-test");
        try {
            var ctx = scanner.scan(tmpDir, null);
            assertThat(ctx.files()).isEmpty();
        } finally {
            java.nio.file.Files.deleteIfExists(tmpDir);
        }
    }
}
