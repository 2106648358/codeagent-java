package com.edianyun.codeagentjava.domain.model.session;

import java.util.UUID;

/**
 * 会话 ID 值对象，用于唯一标识一个对话会话。
 * 使用 UUID 字符串作为底层表示，确保全局唯一性。
 */
public record SessionId(String value) {

    public SessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Session id must not be blank");
        }
    }

    /** 生成一个新的随机会话 ID */
    public static SessionId generate() {
        return new SessionId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
