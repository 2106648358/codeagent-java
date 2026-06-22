package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.FileChange;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.repository.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地文件系统仓库适配器，实现 FileRepository 端口。
 * 支持工作区扫描、文件读取、安全写入（含备份）、dry-run 预检，
 * 并限制文件写入不得超出工作区目录。
 */
public class LocalFileSystemFileRepository implements FileRepository {

    @Override
    public WorkspaceContext scan(Path workingDir, ScanOptions options) {
        ScanOptions scanOptions = options != null ? options : ScanOptions.defaults();
        List<RelativePath> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(workingDir, scanOptions.maxDepth())) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> shouldInclude(p, workingDir, scanOptions))
                    .forEach(p -> files.add(relativize(workingDir, p)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan workspace: " + workingDir, e);
        }
        ContentType primaryType = files.isEmpty() ? ContentType.UNKNOWN : ContentType.fromExtension(extensionOf(files.get(0).value()));
        return new WorkspaceContext(workingDir, files, List.of(), primaryType, null);
    }

    @Override
    public List<ContentFragment> readFiles(List<RelativePath> paths) {
        List<ContentFragment> fragments = new ArrayList<>();
        for (RelativePath path : paths) {
            Path base = Path.of(".").toAbsolutePath().normalize();
            Path file = path.toPath(base);
            try {
                String content = Files.readString(file);
                ContentType type = ContentType.fromExtension(extensionOf(path.value()));
                fragments.add(ContentFragment.of(path, content, type));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + file, e);
            }
        }
        return fragments;
    }

    @Override
    public void apply(FileChange change, WriteOptions options) {
        WriteOptions writeOptions = options != null ? options : WriteOptions.defaults();
        Path base = Path.of(".").toAbsolutePath().normalize();
        Path target = change.path().toPath(base);
        ensureWithinWorkspace(target, base);

        if (writeOptions.dryRun()) {
            return;
        }

        try {
            Files.createDirectories(target.getParent());
            if (change.type() == FileChange.ChangeType.DELETE) {
                if (writeOptions.backup() && Files.exists(target)) {
                    backup(target);
                }
                Files.deleteIfExists(target);
            } else {
                if (writeOptions.backup() && Files.exists(target)) {
                    backup(target);
                }
                Files.writeString(target, change.newContent(), StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to apply file change: " + target, e);
        }
    }

    @Override
    public void dryRun(List<FileChange> changes) {
        Path base = Path.of(".").toAbsolutePath().normalize();
        for (FileChange change : changes) {
            Path target = change.path().toPath(base);
            ensureWithinWorkspace(target, base);
            if (change.type() == FileChange.ChangeType.DELETE) {
                if (!Files.exists(target)) {
                    throw new RuntimeException("Cannot delete non-existent file: " + target);
                }
            }
        }
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

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1);
    }

    private void ensureWithinWorkspace(Path target, Path base) {
        if (!target.normalize().startsWith(base.normalize())) {
            throw new RuntimeException("Target path is outside workspace: " + target);
        }
    }

    private void backup(Path target) throws IOException {
        String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path backup = Path.of(target.toString() + ".backup." + suffix);
        Files.copy(target, backup);
    }
}
