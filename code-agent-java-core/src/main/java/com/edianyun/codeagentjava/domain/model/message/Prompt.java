package com.edianyun.codeagentjava.domain.model.message;

/**
 * Prompt 值对象，封装发送给 LLM 的提示内容及其角色。
 * 与 Message 不同，Prompt 更侧重于 LLM 请求的语义结构，
 * 用于 PromptBuilder 构建 LLM 调用时的输入。
 */
public record Prompt(String role, String content) {

    public Prompt {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Prompt role must not be blank");
        }
        if (content == null) {
            throw new IllegalArgumentException("Prompt content must not be null");
        }
    }

    /** 创建用户角色的 Prompt */
    public static Prompt user(String content) {
        return new Prompt("user", content);
    }

    /** 创建系统角色的 Prompt */
    public static Prompt system(String content) {
        return new Prompt("system", content);
    }

    /** 创建助手角色的 Prompt */
    public static Prompt assistant(String content) {
        return new Prompt("assistant", content);
    }
}
