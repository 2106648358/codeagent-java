package com.edianyun.codeagentjava.application.dto;

/**
 * 聊天请求 DTO，封装来自 CLI 的聊天命令参数。
 * sessionId 和 userId 可选（为空时自动生成或使用默认值）。
 */
public record ChatRequest(String sessionId, String userId, String prompt) {

    public ChatRequest {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }
    }
}
