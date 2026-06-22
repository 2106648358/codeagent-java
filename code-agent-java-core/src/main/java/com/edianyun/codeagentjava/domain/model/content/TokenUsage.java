package com.edianyun.codeagentjava.domain.model.content;

/**
 * Token 用量值对象，记录一次 LLM 调用的输入/输出 Token 数量。
 * 用于遥测统计和成本估算。
 */
public record TokenUsage(int inputTokens, int outputTokens) {

    public TokenUsage {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("Input tokens must not be negative");
        }
        if (outputTokens < 0) {
            throw new IllegalArgumentException("Output tokens must not be negative");
        }
    }

    /** 计算总 Token 数 */
    public int totalTokens() {
        return inputTokens + outputTokens;
    }
}
