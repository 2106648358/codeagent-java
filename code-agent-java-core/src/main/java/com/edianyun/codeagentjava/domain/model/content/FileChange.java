package com.edianyun.codeagentjava.domain.model.content;

import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;

/**
 * 文件变更值对象，描述对单个文件的修改操作。
 * 包含新旧内容和变更类型，用于展示 diff 和执行写入。
 */
public record FileChange(RelativePath path, String oldContent, String newContent, ChangeType type) {

    public FileChange {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (newContent == null) {
            throw new IllegalArgumentException("New content must not be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Change type must not be null");
        }
    }

    /** 创建更新类型的文件变更 */
    public static FileChange update(RelativePath path, String oldContent, String newContent) {
        return new FileChange(path, oldContent == null ? "" : oldContent, newContent, ChangeType.UPDATE);
    }

    /** 创建新增类型的文件变更 */
    public static FileChange create(RelativePath path, String newContent) {
        return new FileChange(path, null, newContent, ChangeType.CREATE);
    }

    /** 创建删除类型的文件变更 */
    public static FileChange delete(RelativePath path, String oldContent) {
        return new FileChange(path, oldContent == null ? "" : oldContent, "", ChangeType.DELETE);
    }

    /** 变更类型：新增/修改/删除 */
    public enum ChangeType {
        CREATE,
        UPDATE,
        DELETE
    }
}
