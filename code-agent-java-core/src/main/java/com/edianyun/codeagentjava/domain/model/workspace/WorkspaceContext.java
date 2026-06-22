package com.edianyun.codeagentjava.domain.model.workspace;

import com.edianyun.codeagentjava.domain.model.content.ContentType;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * 工作区上下文实体，包含用户工作目录的快照信息。
 * 记录了文件列表、选中文件、主要内容类型和依赖线索，
 * 用于为 LLM 提供上下文信息以生成更准确的回复。
 */
public class WorkspaceContext {

    private final Path workingDir;
    private final List<RelativePath> files;
    private final List<RelativePath> selectedFiles;
    private final ContentType primaryContentType;
    private final String dependencyHints;

    public WorkspaceContext(Path workingDir, List<RelativePath> files, List<RelativePath> selectedFiles, ContentType primaryContentType, String dependencyHints) {
        this.workingDir = workingDir;
        this.files = files == null ? List.of() : List.copyOf(files);
        this.selectedFiles = selectedFiles == null ? List.of() : List.copyOf(selectedFiles);
        this.primaryContentType = primaryContentType;
        this.dependencyHints = dependencyHints;
    }

    public Path workingDir() { return workingDir; }

    /** 工作区内所有文件的相对路径列表 */
    public List<RelativePath> files() { return Collections.unmodifiableList(files); }

    /** 用户选中的上下文文件列表 */
    public List<RelativePath> selectedFiles() { return Collections.unmodifiableList(selectedFiles); }

    /** 工作区的主要内容类型 */
    public ContentType primaryContentType() { return primaryContentType; }

    /** 依赖信息线索（如包管理器、构建工具等） */
    public String dependencyHints() { return dependencyHints; }
}
