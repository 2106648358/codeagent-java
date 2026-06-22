package com.edianyun.codeagentjava.domain.service.impl;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.service.ContentTypeResolver;
import com.edianyun.codeagentjava.domain.service.WorkspaceScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 默认工作区扫描器实现。
 * 递归扫描工作目录，根据 ScanOptions 过滤文件，
 * 并使用 ContentTypeResolver 推断主要内容类型。
 */
public class DefaultWorkspaceScanner implements WorkspaceScanner {

    private final ContentTypeResolver contentTypeResolver;

    public DefaultWorkspaceScanner(ContentTypeResolver contentTypeResolver) {
        this.contentTypeResolver = contentTypeResolver;
    }

    @Override
    public WorkspaceContext scan(Path workingDir, ScanOptions options) {
        ScanOptions scanOptions = options != null ? options : ScanOptions.defaults();
        List<RelativePath> files = new ArrayList<>();
        ContentType primaryType = ContentType.CODE;
        try (Stream<Path> stream = Files.walk(workingDir, scanOptions.maxDepth())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> shouldInclude(p, workingDir, scanOptions))
                    .forEach(p -> files.add(relativize(workingDir, p)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan workspace: " + workingDir, e);
        }
        if (!files.isEmpty()) {
            primaryType = contentTypeResolver.resolve(files.get(0).value());
        }
        return new WorkspaceContext(workingDir, files, List.of(), primaryType, null);
    }

    private boolean shouldInclude(Path file, Path workingDir, ScanOptions options) {
        RelativePath relative = relativize(workingDir, file);
        String value = relative.value();
        if (!options.includeDotFiles() && (value.startsWith(".") || value.contains("/."))) {
            return false;
        }
        if (options.includePattern() != null && !value.matches(options.includePattern())) {
            return false;
        }
        if (options.excludePattern() != null && value.matches(options.excludePattern())) {
            return false;
        }
        return true;
    }

    private RelativePath relativize(Path workingDir, Path file) {
        return new RelativePath(workingDir.relativize(file).toString().replace("\\", "/"));
    }
}
