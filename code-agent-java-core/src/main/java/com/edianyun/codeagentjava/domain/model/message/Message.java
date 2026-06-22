package com.edianyun.codeagentjava.domain.model.message;

import com.edianyun.codeagentjava.domain.model.content.ModelId;
import com.edianyun.codeagentjava.domain.model.content.TokenUsage;

import java.time.Instant;

/**
 * 消息值对象，表示一轮对话中的单条消息。
 * 包含角色、文本内容、使用的模型和 Token 消耗等元信息。
 */
public record Message(Role role, String content, ModelId modelId, TokenUsage tokenUsage, Instant timestamp) {

    public Message {
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
    }

    /** 快速构建一条用户消息 */
    public static Message user(String content) {
        return new Message(Role.USER, content, null, null, Instant.now());
    }

    /** 快速构建一条助手回复消息，可附带模型 ID 和 Token 用量 */
    public static Message assistant(String content, ModelId modelId, TokenUsage tokenUsage) {
        return new Message(Role.ASSISTANT, content, modelId, tokenUsage, Instant.now());
    }
}
