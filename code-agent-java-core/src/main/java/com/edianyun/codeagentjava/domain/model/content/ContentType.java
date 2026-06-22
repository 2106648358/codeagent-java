package com.edianyun.codeagentjava.domain.model.content;

/**
 * 内容类型枚举，标识文件或内容的种类。
 * 用于区分代码、文档、配置文件等不同内容类型，
 * 以便选择不同的处理和渲染策略。
 */
public enum ContentType {

    CODE,
    MARKDOWN,
    JSON,
    YAML,
    CONFIG,
    TEXT,
    UNKNOWN;

    /** 根据文件扩展名推断内容类型 */
    public static ContentType fromExtension(String extension) {
        if (extension == null) {
            return UNKNOWN;
        }
        return switch (extension.toLowerCase()) {
            case "md", "markdown" -> MARKDOWN;
            case "json" -> JSON;
            case "yml", "yaml" -> YAML;
            case "txt", "text" -> TEXT;
            default -> CODE;
        };
    }
}
