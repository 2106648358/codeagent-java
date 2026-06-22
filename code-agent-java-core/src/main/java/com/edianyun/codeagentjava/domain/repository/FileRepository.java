package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.FileChange;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;

import java.nio.file.Path;
import java.util.List;

/**
 * 文件仓库端口（出站端口），定义对本地文件系统的操作。
 * 包括工作区扫描、文件读取、变更应用和预检，
 * 将文件 I/O 操作与领域逻辑解耦。
 */
public interface FileRepository {

    /** 扫描工作目录，返回工作区上下文 */
    WorkspaceContext scan(Path workingDir, ScanOptions options);

    /** 读取指定文件列表的内容 */
    List<ContentFragment> readFiles(List<RelativePath> paths);

    /** 应用单个文件变更（写入磁盘） */
    void apply(FileChange change, WriteOptions options);

    /** 预检文件变更列表（检查文件是否存在等） */
    void dryRun(List<FileChange> changes);

    /** 扫描选项：最大深度、是否包含点文件、包含/排除模式 */
    record ScanOptions(int maxDepth, boolean includeDotFiles, String includePattern, String excludePattern) {

        public static ScanOptions defaults() {
            return new ScanOptions(5, false, null, null);
        }
    }

    /** 写入选项：是否备份、是否为 dry-run */
    record WriteOptions(boolean backup, boolean dryRun) {

        public static WriteOptions defaults() {
            return new WriteOptions(true, false);
        }

        public static WriteOptions forDryRun() {
            return new WriteOptions(false, true);
        }
    }
}
