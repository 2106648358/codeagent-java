package com.edianyun.codeagentjava.domain.model.workspace;

import java.nio.file.Path;

/**
 * 相对路径值对象，表示工作区内的文件相对路径。
 * 强制要求路径不以 "/" 或 "\\" 开头以确保是相对路径。
 */
public record RelativePath(String value) {

    public RelativePath {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Relative path must not be blank");
        }
        if (value.startsWith("/") || value.startsWith("\\")) {
            throw new IllegalArgumentException("Relative path must not be absolute: " + value);
        }
    }

    /** 基于工作区基路径解析为绝对路径 */
    public Path toPath(Path base) {
        return base.resolve(value).normalize();
    }

    @Override
    public String toString() {
        return value;
    }
}
