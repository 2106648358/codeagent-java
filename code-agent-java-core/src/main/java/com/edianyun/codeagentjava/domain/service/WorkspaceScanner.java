package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;

import java.nio.file.Path;

/**
 * 工作区扫描器服务接口，扫描工作目录并收集文件信息。
 * 用于为 LLM 提供当前项目的上下文概览。
 */
public interface WorkspaceScanner {

    /** 扫描工作目录，返回工作区上下文（含文件列表和内容类型） */
    WorkspaceContext scan(Path workingDir, ScanOptions options);

    /** 扫描选项：最大深度、是否包含点文件、包含/排除模式 */
    record ScanOptions(int maxDepth, boolean includeDotFiles, String includePattern, String excludePattern) {

        public static ScanOptions defaults() {
            return new ScanOptions(5, false, null, null);
        }
    }
}
