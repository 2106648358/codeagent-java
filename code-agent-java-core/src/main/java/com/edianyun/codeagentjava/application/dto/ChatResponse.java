package com.edianyun.codeagentjava.application.dto;

/**
 * 聊天响应 DTO，包含会话 ID 和助手的回复内容。
 */
public record ChatResponse(String sessionId, String reply) {

    public ChatResponse {
        if (reply == null) {
            throw new IllegalArgumentException("Reply must not be null");
        }
    }
}
