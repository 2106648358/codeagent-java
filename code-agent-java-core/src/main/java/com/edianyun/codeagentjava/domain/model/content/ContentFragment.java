package com.edianyun.codeagentjava.domain.model.content;

import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;

import java.time.Instant;

/**
 * 内容片段值对象，表示 LLM 生成的一份内容。
 * 包含目标文件路径、内容正文、说明描述和内容类型。
 * 一个 GenerationTask 可产出多个 ContentFragment（如多个文件）。
 */
public record ContentFragment(RelativePath path, String content, String description, ContentType contentType, Instant createdAt) {

    public ContentFragment {
        if (path == null) {
            throw new IllegalArgumentException("Path must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt must not be null");
        }
    }

    /** 快速创建内容片段，不含描述说明 */
    public static ContentFragment of(RelativePath path, String content, ContentType contentType) {
        return new ContentFragment(path, content, null, contentType, Instant.now());
    }

    /** 设置描述说明（不可变模式，返回新实例） */
    public ContentFragment withDescription(String description) {
        return new ContentFragment(path, content, description, contentType, createdAt);
    }
}
